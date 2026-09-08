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
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
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
    private final JdbcTemplate jdbcTemplate;

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
        Specification<SseConnectionRecord> filters = pageDTO == null
                ? (root, query, cb) -> cb.conjunction()
                : SpecUtils.<SseConnectionRecord>builder()
                .eqIfPresent(SseConnectionRecord::getUserId, pageDTO.getUserId())
                .eqIfPresent(SseConnectionRecord::getConnectionStatus, pageDTO.getConnectionStatus())
                .eqIfPresent(SseConnectionRecord::getConnectionId, pageDTO.getConnectionId())
                .likeIfPresent(SseConnectionRecord::getClientIp, pageDTO.getClientIp())
                .geTimeIfPresent(SseConnectionRecord::getConnectTime, pageDTO.getStartTime())
                .leTimeIfPresent(SseConnectionRecord::getConnectTime, pageDTO.getEndTime())
                .build();
        return filters.and(SuperAdminSseVisibility.userIdSpecification());
    }

    @Override
    protected SseConnectionRecord requireEntity(String id) {
        SseConnectionRecord entity = super.requireEntity(id);
        SuperAdminSseVisibility.requireVisible(entity.getUserId(), entity.getCreateBy(), entity.getUpdateBy(),
                getEntityName(), id);
        return entity;
    }

    // ==================== 连接生命周期记录 ====================

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 5)
    public void logConnect(String connectionId, String userId, String tenantId,
                           LocalDateTime connectTime, String clientIp) {
        acquireLifecycleLock(tenantId, connectionId);
        repository.findByTenantIdAndConnectionIdAndDeletedFalse(tenantId, connectionId)
                .ifPresentOrElse(record -> completeMissingMetadata(
                        record, userId, connectTime, clientIp), () -> {
            SseConnectionRecord record = new SseConnectionRecord();
            record.setConnectionId(connectionId);
            record.setUserId(userId);
            record.setTenantId(tenantId);
            record.setConnectionStatus(SseConstants.ConnectionStatus.CONNECTED);
            record.setConnectTime(connectTime);
            record.setClientIp(clientIp);
            record.setServerInstance(getServerInstance());

            repository.save(record);
            log.debug("[SSE-Audit] 连接记录已创建: userId={}, connId={}", userId, connectionId);
        });
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 5)
    public void logDisconnect(String connectionId, String userId, String tenantId,
                              LocalDateTime connectTime, LocalDateTime disconnectTime,
                              String disconnectReason) {
        acquireLifecycleLock(tenantId, connectionId);
        repository.findByTenantIdAndConnectionIdAndDeletedFalse(tenantId, connectionId)
                .ifPresentOrElse(record -> recordDisconnect(record, disconnectTime, disconnectReason), () -> {
                    SseConnectionRecord record = new SseConnectionRecord();
                    record.setConnectionId(connectionId);
                    record.setUserId(userId);
                    record.setTenantId(tenantId);
                    record.setConnectTime(connectTime);
                    record.setServerInstance(getServerInstance());
                    recordDisconnect(record, disconnectTime, disconnectReason);
                });
    }

    // ==================== 查询统计 ====================

    @Override
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        Specification<SseConnectionRecord> visible = SuperAdminSseVisibility.userIdSpecification();
        stats.put("connected", repository.count(visible.and(statusIs(SseConstants.ConnectionStatus.CONNECTED))));
        stats.put("disconnected", repository.count(visible.and(statusIs(SseConstants.ConnectionStatus.DISCONNECTED))));
        stats.put("error", repository.count(visible.and(statusIs(SseConstants.ConnectionStatus.ERROR))));
        stats.put("timeout", repository.count(visible.and(statusIs(SseConstants.ConnectionStatus.TIMEOUT))));
        stats.put("total", repository.count(visible));
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupHistory(int retainDays) {
        LocalDateTime cutoff = LocalDate.now().minusDays(retainDays).atStartOfDay();
        Page<SseConnectionRecord> page = repository.findAll(
                SuperAdminSseVisibility.<SseConnectionRecord>userIdSpecification()
                        .and((root, query, cb) -> cb.lessThan(root.get("connectTime"), cutoff)),
                PageRequest.of(0, properties.historyCleanupBatchSize()));
        List<SseConnectionRecord> oldRecords = page.getContent();
        if (!oldRecords.isEmpty()) {
            repository.deleteAll(oldRecords);
            log.info("[SSE-Audit] 清理历史连接记录: count={}, before={}", oldRecords.size(), cutoff);
        }
        return oldRecords.size();
    }

    private Specification<SseConnectionRecord> statusIs(String status) {
        return (root, query, cb) -> cb.equal(root.get("connectionStatus"), status);
    }

    // ==================== 私有方法 ====================

    /**
     * 以租户和连接标识获取 PostgreSQL 事务级咨询锁。
     *
     * <p>锁在当前 {@code @Transactional} 事务提交或回滚时自动释放，保证异步连接和断开事件
     * 在同一生命周期键上串行，不会因进程重启遗留锁。</p>
     */
    private void acquireLifecycleLock(String tenantId, String connectionId) {
        String lockKey = lengthPrefixedKey(tenantId, connectionId);
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))")) {
                statement.setQueryTimeout(5);
                statement.setString(1, lockKey);
                statement.execute();
            }
            return null;
        });
    }

    private String lengthPrefixedKey(String tenantId, String connectionId) {
        return tenantId.length() + ":" + tenantId + connectionId.length() + ":" + connectionId;
    }

    private void completeMissingMetadata(
            SseConnectionRecord record, String userId, LocalDateTime connectTime, String clientIp) {
        boolean changed = false;
        if (!StringUtils.hasText(record.getUserId()) && StringUtils.hasText(userId)) {
            record.setUserId(userId);
            changed = true;
        }
        if (record.getConnectTime() == null && connectTime != null) {
            record.setConnectTime(connectTime);
            changed = true;
        }
        if (!StringUtils.hasText(record.getClientIp()) && StringUtils.hasText(clientIp)) {
            record.setClientIp(clientIp);
            changed = true;
        }
        if (!StringUtils.hasText(record.getServerInstance())) {
            record.setServerInstance(getServerInstance());
            changed = true;
        }
        if (changed) repository.save(record);
    }

    private void recordDisconnect(
            SseConnectionRecord record, LocalDateTime disconnectTime, String disconnectReason) {
        if (StringUtils.hasText(record.getConnectionStatus())
                && !SseConstants.ConnectionStatus.CONNECTED.equals(record.getConnectionStatus())) return;
        record.setConnectionStatus(disconnectReason);
        record.setDisconnectTime(disconnectTime);
        record.setDisconnectReason(disconnectReason);
        if (record.getConnectTime() != null) {
            long seconds = ChronoUnit.SECONDS.between(record.getConnectTime(), disconnectTime);
            record.setDurationSeconds(Math.max(0, seconds));
        }
        repository.save(record);
        log.debug("[SSE-Audit] 连接断开已记录: connId={}, reason={}, duration={}s",
                record.getConnectionId(), disconnectReason, record.getDurationSeconds());
    }


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
