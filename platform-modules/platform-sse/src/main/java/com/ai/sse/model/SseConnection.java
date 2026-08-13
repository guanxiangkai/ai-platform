package com.ai.sse.model;

import com.ai.sse.constants.SseConstants;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SSE 连接模型（不可变）
 * <p>
 * 使用 Java Record 表达连接元数据，{@code sink} 字段为 Reactor 推送通道。
 * {@code connectionId} 标识单个连接实例（同一用户可有多个连接，如多标签页/设备）。
 * {@code status} 为字典值字符串（对应字典 {@code sse_connection_status}）。
 * 状态变更通过 wither 方法返回新实例，保证线程安全。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RegisterReflectionForBinding
public record SseConnection(
        String connectionId,
        String userId,
        Sinks.Many<String> sink,
        String tenantId,
        String status,
        LocalDateTime connectTime,
        LocalDateTime lastActiveTime
) {

    /**
     * 紧凑型构造器 — 填充默认值
     */
    public SseConnection {
        if (connectionId == null) {
            connectionId = UUID.randomUUID().toString().replace("-", "");
        }
        if (status == null) {
            status = SseConstants.ConnectionStatus.CONNECTED;
        }
        if (connectTime == null) {
            connectTime = LocalDateTime.now();
        }
        if (lastActiveTime == null) {
            lastActiveTime = LocalDateTime.now();
        }
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建新连接
     */
    public static SseConnection of(String userId, Sinks.Many<String> sink, String tenantId) {
        return new SseConnection(null, userId, sink, tenantId, SseConstants.ConnectionStatus.CONNECTED, null, null);
    }

    // ==================== Wither 方法（返回新实例） ====================

    /**
     * 更新最后活跃时间
     */
    public SseConnection updateActiveTime() {
        return new SseConnection(connectionId, userId, sink, tenantId, status, connectTime, LocalDateTime.now());
    }

    /**
     * 变更连接状态
     */
    public SseConnection withStatus(String newStatus) {
        return new SseConnection(connectionId, userId, sink, tenantId, newStatus, connectTime, lastActiveTime);
    }

    /**
     * 判断连接是否已超时
     *
     * @param timeoutMillis 超时毫秒数
     */
    public boolean isExpired(long timeoutMillis) {
        return lastActiveTime.plusNanos(timeoutMillis * 1_000_000).isBefore(LocalDateTime.now());
    }
}
