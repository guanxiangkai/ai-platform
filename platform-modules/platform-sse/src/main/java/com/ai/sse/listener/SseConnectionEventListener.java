package com.ai.sse.listener;

import com.ai.sse.event.SseConnectEvent;
import com.ai.sse.event.SseDisconnectEvent;
import com.ai.sse.model.SseConnection;
import com.ai.sse.service.ISseConnectionRecordService;
import com.ai.sse.service.ISseLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * SSE 连接事件监听器（观察者模式）
 * <p>
 * 监听 {@link SseConnectEvent} 和 {@link SseDisconnectEvent}，同时负责：
 * <ul>
 *   <li>更新可重试的 {@code SseConnectionRecord}（mutable，记录连接生命周期）</li>
 *   <li>写入不可变的 {@code SseLog}（immutable，审计每次连接/断开事件）</li>
 * </ul>
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseConnectionEventListener {

    private final ISseConnectionRecordService connectionRecordService;
    private final ISseLogService sseLogService;

    /**
     * 监听 SSE 连接建立事件
     * — 更新 SseConnectionRecord（可变状态记录）
     * — 写入 SseLog CONNECT 条目（不可变审计）
     */
    @EventListener
    public void onConnect(SseConnectEvent event) {
        SseConnection connection = event.getConnection();
        // 1. 可变连接记录（生命周期状态）
        connectionRecordService.logConnect(
                connection.connectionId(),
                connection.userId(),
                connection.tenantId(),
                connection.connectTime(),
                null
        );
        // 2. 不可变审计日志
        sseLogService.logConnect(
                connection.connectionId(),
                connection.userId(),
                connection.tenantId(),
                null,
                connection.connectTime()
        );
    }

    /**
     * 监听 SSE 连接断开事件
     * — 更新 SseConnectionRecord（可变状态记录）
     * — 写入 SseLog DISCONNECT 条目（不可变审计）
     */
    @EventListener
    public void onDisconnect(SseDisconnectEvent event) {
        SseConnection connection = event.getConnection();
        LocalDateTime now = LocalDateTime.now();
        // 1. 可变连接记录
        connectionRecordService.logDisconnect(
                connection.connectionId(),
                now,
                connection.status()
        );
        // 2. 不可变审计日志
        sseLogService.logDisconnect(
                connection.connectionId(),
                connection.userId(),
                connection.tenantId(),
                now,
                connection.status()
        );
    }
}
