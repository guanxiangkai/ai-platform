package com.ai.sse.core;

import com.ai.sse.model.SseConnection;
import com.ai.sse.model.SseMessage;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

/**
 * SSE 操作核心接口（WebFlux 响应式版本）
 * <p>
 * 生命周期事件（连接/断开/消息）通过 Spring {@code ApplicationEvent} 发布，
 * 订阅方使用 {@code @EventListener} 监听，不再通过 {@code onConnect/onDisconnect/onMessage} 注册。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface SseOperations {

    // ==================== 连接管理 ====================

    Flux<ServerSentEvent<String>> connect(String userId, String tenantId);

    void disconnect(String userId);

    Optional<SseConnection> getConnection(String userId);

    boolean isOnline(String userId);

    int getOnlineCount();

    int getOnlineCountByTenant(String tenantId);

    // ==================== 连接维护 ====================

    /**
     * 向本实例的全部在线连接发送心跳，并回收发送失败的连接。
     *
     * <p>调度器通过此契约调用所有实现；替换实现时必须保留连接维护语义，
     * 不得把心跳广播到其他实例。实现需允许连接同时建立、取消和发送消息。</p>
     *
     * @return 成功发送心跳的连接数，同一用户的多个连接分别计数
     */
    int sendHeartbeatToAll();

    /**
     * 按实现配置的超时策略回收本实例的过期连接。
     *
     * <p>实现需允许与连接建立、心跳和断开并发执行，重复清理不得重复释放资源。</p>
     *
     * @return 本次清理的连接数
     */
    int cleanupExpiredConnections();

    // ==================== 消息发送 ====================

    void sendToUser(String userId, SseMessage<?> message);

    void sendToUsers(List<String> userIds, SseMessage<?> message);

    void broadcast(SseMessage<?> message);

    void broadcastToTenant(String tenantId, SseMessage<?> message);

    void sendToGroup(String groupId, List<String> userIds, SseMessage<?> message);
}
