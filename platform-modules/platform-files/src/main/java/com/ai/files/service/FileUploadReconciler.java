package com.ai.files.service;

import com.ai.files.config.FilesProperties;
import com.ai.files.domain.FileUploadState;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 逐租户恢复超时上传或重试孤立对象清理。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
class FileUploadReconciler {
    private final JdbcTemplate jdbcTemplate;
    private final FileUploadTransactionService transactions;
    private final FileObjectStorage objectStorage;
    private final FilesProperties properties;

    /** 定期处理已超过执行租约的未完成上传。 */
    @Scheduled(fixedDelayString = "${platform.files.reconcile-interval:30s}")
    public void reconcile() {
        candidates().forEach(this::reconcileCandidate);
    }

    private List<Candidate> candidates() {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, operator_user_id
                FROM public.file_upload_record
                WHERE deleted = false
                  AND upload_state IN ('PREPARED', 'OBJECT_STORED', 'CLEANUP_PENDING')
                  AND lease_expires_at <= now()
                  AND next_attempt_at <= now()
                ORDER BY next_attempt_at, create_time
                LIMIT ?
                """, (result, row) -> new Candidate(
                result.getString("id"),
                result.getString("tenant_id"),
                result.getString("operator_user_id")), properties.resolvedReconcileBatchSize());
    }

    private void reconcileCandidate(Candidate candidate) {
        UserContext previous = UserContextHolder.get();
        UserContextHolder.set(new UserContext(
                candidate.userId(), candidate.tenantId(), false, null,
                Set.of(), Set.of(), Set.of(), Map.of()));
        try {
            try {
                FileUploadTransactionService.Recovery recovery =
                        transactions.claim(candidate.uploadId(), candidate.tenantId());
                if (recovery != null) {
                    recover(recovery);
                }
            } catch (RuntimeException error) {
                log.warn("文件上传恢复记录领取失败: uploadId={}, exception={}",
                        candidate.uploadId(), error.getClass().getSimpleName());
            }
        } finally {
            if (previous == null) {
                UserContextHolder.clear();
            } else {
                UserContextHolder.set(previous);
            }
        }
    }

    void recover(FileUploadTransactionService.Recovery recovery) {
        var reservation = recovery.reservation();
        try {
            if (recovery.state() == FileUploadState.PREPARED) {
                if (!objectStorage.exists(reservation.objectKey())) {
                    transactions.cancel(reservation);
                    return;
                }
                transactions.markObjectStored(reservation);
                transactions.complete(reservation);
                return;
            }
            if (recovery.state() == FileUploadState.OBJECT_STORED) {
                transactions.complete(reservation);
                return;
            }
            if (recovery.state() == FileUploadState.CLEANUP_PENDING) {
                objectStorage.delete(reservation.objectKey());
                transactions.cancel(reservation);
            }
        } catch (RuntimeException error) {
            try {
                transactions.recordRecoveryFailure(reservation, error);
            } catch (RuntimeException persistenceFailure) {
                error.addSuppressed(persistenceFailure);
            }
            log.warn("文件上传恢复失败: uploadId={}, state={}, exception={}",
                    reservation.uploadId(), recovery.state(), error.getClass().getSimpleName());
        }
    }

    private record Candidate(String uploadId, String tenantId, String userId) {
    }
}
