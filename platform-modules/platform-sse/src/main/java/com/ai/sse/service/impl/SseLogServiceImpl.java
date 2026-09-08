package com.ai.sse.service.impl;

import com.ai.sse.domain.SseOperationType;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import com.ai.sse.domain.dto.SseLogDTO;
import com.ai.sse.domain.dto.SseLogPageDTO;
import com.ai.sse.domain.entity.SseLog;
import com.ai.sse.domain.vo.SseLogPageVO;
import com.ai.sse.domain.vo.SseLogVO;
import com.ai.sse.repository.SseLogRepository;
import com.ai.sse.service.ISseLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SSE 操作日志服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseLogServiceImpl
        extends BaseServiceImpl<SseLogPageDTO, SseLogPageVO, SseLogVO, SseLogDTO, SseLogDTO, SseLog>
        implements ISseLogService {


    private final SseLogRepository repository;

    @Override
    protected BaseRepository<SseLogPageVO, SseLogVO, SseLog> getRepository() {
        return this.repository;
    }

    @Override
    protected Sort buildSort(SseLogPageDTO pageDTO) {
        return Sort.by(Sort.Order.desc("logTime"));
    }

    @Override
    protected Specification<SseLog> buildQuerySpec(SseLogPageDTO pageDTO) {
        if (pageDTO == null) return SuperAdminSseVisibility.userIdSpecification();
        return SpecUtils.<SseLog>builder()
                .eqIfPresent(SseLog::getOperationType, pageDTO.getOperationType())
                .eqIfPresent(SseLog::getMessageType, pageDTO.getMessageType())
                .eqIfPresent(SseLog::getTargetType, pageDTO.getTargetType())
                .likeIfPresent(SseLog::getUsername, pageDTO.getUsername())
                .eqIfPresent(SseLog::getStatus, pageDTO.getStatus())
                .geTimeIfPresent(SseLog::getLogTime, pageDTO.getStartTime())
                .leTimeIfPresent(SseLog::getLogTime, pageDTO.getEndTime())
                .build()
                .and(SuperAdminSseVisibility.userIdSpecification());
    }

    @Override
    protected SseLog requireEntity(String id) {
        SseLog entity = super.requireEntity(id);
        SuperAdminSseVisibility.requireVisible(entity.getUserId(), entity.getCreateBy(), entity.getUpdateBy(),
                getEntityName(), id);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createEntity(SseLog entity) {
        repository.save(entity);
    }

    // ==================== 连接事件不可变日志 ====================

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logConnect(String connectionId, String userId, String tenantId,
                           String clientIp, LocalDateTime connectTime) {
        try {
            SseLog entry = new SseLog();
            entry.setOperationType(SseOperationType.CONNECT);
            entry.setConnectionId(connectionId);
            entry.setUserId(userId);
            entry.setTenantId(tenantId);
            entry.setClientIp(clientIp);
            entry.setStatus("SUCCESS");
            entry.setLogTime(connectTime != null ? connectTime : LocalDateTime.now());
            entry.setDescription("SSE 连接建立");
            repository.save(entry);
            log.debug("[SseLog] CONNECT 已记录: connId={}, userId={}", connectionId, userId);
        } catch (Exception e) {
            log.error("[SseLog] 记录 CONNECT 失败: connId={}", connectionId, e);
        }
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logDisconnect(String connectionId, String userId, String tenantId,
                              LocalDateTime disconnectTime, String disconnectReason) {
        try {
            SseLog entry = new SseLog();
            entry.setOperationType(SseOperationType.DISCONNECT);
            entry.setConnectionId(connectionId);
            entry.setUserId(userId);
            entry.setTenantId(tenantId);
            entry.setStatus("SUCCESS");
            entry.setLogTime(disconnectTime != null ? disconnectTime : LocalDateTime.now());
            entry.setDescription("SSE 连接断开：" + disconnectReason);
            entry.setFailReason(disconnectReason);
            repository.save(entry);
            log.debug("[SseLog] DISCONNECT 已记录: connId={}, reason={}", connectionId, disconnectReason);
        } catch (Exception e) {
            log.error("[SseLog] 记录 DISCONNECT 失败: connId={}", connectionId, e);
        }
    }
}
