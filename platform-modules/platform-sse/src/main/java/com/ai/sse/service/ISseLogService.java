package com.ai.sse.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.sse.domain.dto.SseLogDTO;
import com.ai.sse.domain.dto.SseLogPageDTO;
import com.ai.sse.domain.entity.SseLog;
import com.ai.sse.domain.vo.SseLogPageVO;
import com.ai.sse.domain.vo.SseLogVO;

import java.time.LocalDateTime;

/**
 * SSE 操作日志服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface ISseLogService extends IBaseService<SseLogPageDTO, SseLogPageVO, SseLogVO, SseLogDTO, SseLogDTO, SseLog> {

    /**
     * 直接持久化已构建完成的 SSE 日志实体
     *
     * @param entity SSE 日志实体
     */
    void createEntity(SseLog entity);

    /**
     * 记录 SSE 连接建立的不可变审计日志
     *
     * @param connectionId 连接唯一标识
     * @param userId       用户 ID
     * @param tenantId     租户 ID
     * @param clientIp     客户端 IP
     * @param connectTime  连接时间
     */
    void logConnect(String connectionId, String userId, String tenantId,
                    String clientIp, LocalDateTime connectTime);

    /**
     * 记录 SSE 连接断开的不可变审计日志
     *
     * @param connectionId     连接唯一标识
     * @param disconnectTime   断开时间
     * @param disconnectReason 断开原因
     */
    void logDisconnect(String connectionId, LocalDateTime disconnectTime, String disconnectReason);
}
