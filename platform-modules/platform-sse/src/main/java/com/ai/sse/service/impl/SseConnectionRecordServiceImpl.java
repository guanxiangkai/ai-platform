package com.ai.sse.service.impl;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import com.ai.sse.constants.SseConstants;
import com.ai.sse.config.SseProperties;
import com.ai.sse.domain.dto.SseConnectionRecordDTO;
import com.ai.sse.domain.dto.SseConnectionRecordPageDTO;
import com.ai.sse.domain.entity.SseConnectionRecord;
import com.ai.sse.domain.vo.SseConnectionRecordPageVO;
import com.ai.sse.domain.vo.SseConnectionRecordVO;
import com.ai.sse.repository.SseConnectionRecordRepository;
import com.ai.sse.service.ISseConnectionRecordService;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SSE 连接记录服务实现
 * <p>
 * 继承 {@link BaseServiceImpl} 获得标准 CRUD，
 * 同时实现连接生命周期记录和审计查询功能。
 * 写入操作使用 {@code @Async} 异步执行，避免阻塞 SSE 连接/断开的主流程。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseConnectionRecordServiceImpl
        extends BaseServiceImpl<SseConnectionRecordPageDTO, SseConnectionRecordPageVO,
        SseConnectionRecordVO, SseConnectionRecordDTO, SseConnectionRecordDTO, SseConnectionRecord>
        implements ISseConnectionRecordService {


    private final SseConnectionRecordRepository repository;
    private final SseProperties properties;

    @Override
    protected BaseRepository<SseConnectionRecordPageVO, SseConnectionRecordVO, SseConnectionRecord> getRepository() {
        return this.repository;
    }

    @Override
    protected Sort buildSort(SseConnectionRecordPageDTO pageDTO) {
        return Sort.by(Sort.Order.desc("connectTime"));
    }

    @Override
    protected Specification<SseConnectionRecord> buildQuerySpec(SseConnectionRecordPageDTO pageDTO) {
        if (pageDTO == null) return (root, query, cb) -> cb.conjunction();
        return SpecUtils.<SseConnectionRecord>builder()
                .eqIfPresent(SseConnectionRecord::getUserId, pageDTO.getUserId())
                .eqIfPresent(SseConnectionRecord::getConnectionStatus, pageDTO.getConnectionStatus())
                .eqIfPresent(SseConnectionRecord::getConnectionId, pageDTO.getConnectionId())
                .likeIfPresent(SseConnectionRecord::getClientIp, pageDTO.getClientIp())
                .geTimeIfPresent(SseConnectionRecord::getConnectTime, pageDTO.getStartTime())
                .leTimeIfPresent(SseConnectionRecord::getConnectTime, pageDTO.getEndTime())
                .build();
    }

    // ==================== 连接生命周期记录 ====================

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logConnect(String connectionId, String userId, String tenantId,
                           LocalDateTime connectTime, String clientIp) {
        try {
            SseConnectionRecord record = new SseConnectionRecord();
            record.setConnectionId(connectionId);
            record.setUserId(userId);
            record.setTenantId(tenantId);
            record.setConnectionStatus(SseConstants.ConnectionStatus.CONNECTED);
            record.setConnectTime(connectTime);
            record.setClientIp(clientIp);
            record.setServerInstance(getServerInstance());

            repository.save(record);
            log.debug("[SSE-Audit] 连接记录已创建: connId={}", connectionId);
        } catch (Exception e) {
            log.error("[SSE-Audit] 记录连接失败: connId={}, exception={}",
                    connectionId, e.getClass().getSimpleName());
        }
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logDisconnect(String connectionId, LocalDateTime disconnectTime, String disconnectReason) {
        try {
            repository.findByConnectionIdAndDeletedFalse(connectionId)
                    .ifPresentOrElse(record -> {
                        record.setConnectionStatus(disconnectReason);
                        record.setDisconnectTime(disconnectTime);
                        record.setDisconnectReason(disconnectReason);

                        // 计算连接持续时长
                        if (record.getConnectTime() != null) {
                            long seconds = ChronoUnit.SECONDS.between(record.getConnectTime(), disconnectTime);
                            record.setDurationSeconds(Math.max(0, seconds));
                        }

                        repository.save(record);
                        log.debug("[SSE-Audit] 连接断开已记录: connId={}, reason={}, duration={}s",
                                connectionId, disconnectReason, record.getDurationSeconds());
                    }, () -> log.warn("[SSE-Audit] 断开时未找到连接记录: connId={}", connectionId));
        } catch (Exception e) {
            log.error("[SSE-Audit] 记录断开失败: connId={}, exception={}",
                    connectionId, e.getClass().getSimpleName());
        }
    }

    // ==================== 查询统计 ====================

    @Override
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("connected", repository
                .countByConnectionStatusAndDeletedFalse(SseConstants.ConnectionStatus.CONNECTED));
        stats.put("disconnected", repository
                .countByConnectionStatusAndDeletedFalse(SseConstants.ConnectionStatus.DISCONNECTED));
        stats.put("error", repository
                .countByConnectionStatusAndDeletedFalse(SseConstants.ConnectionStatus.ERROR));
        stats.put("timeout", repository
                .countByConnectionStatusAndDeletedFalse(SseConstants.ConnectionStatus.TIMEOUT));
        stats.put("total", repository.count());
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupHistory(int retainDays) {
        LocalDateTime cutoff = LocalDate.now().minusDays(retainDays).atStartOfDay();
        Page<SseConnectionRecord> page = repository.findAll(
                (root, query, cb) -> cb.lessThan(root.get("connectTime"), cutoff),
                PageRequest.of(0, properties.historyCleanupBatchSize()));
        List<SseConnectionRecord> oldRecords = page.getContent();
        if (!oldRecords.isEmpty()) {
            repository.deleteAll(oldRecords);
            log.info("[SSE-Audit] 清理历史连接记录: count={}, before={}", oldRecords.size(), cutoff);
        }
        return oldRecords.size();
    }

    // ==================== 私有方法 ====================


    /**
     * 获取当前服务实例标识
     */
    private String getServerInstance() {
        try {
            String hostName = java.net.InetAddress.getLocalHost().getHostName();
            String pid = String.valueOf(ProcessHandle.current().pid());
            return hostName + ":" + pid;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
