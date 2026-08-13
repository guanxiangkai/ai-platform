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

    // ==================== 消息发送 ====================

    void sendToUser(String userId, SseMessage<?> message);

    void sendToUsers(List<String> userIds, SseMessage<?> message);

    void broadcast(SseMessage<?> message);

    void broadcastToTenant(String tenantId, SseMessage<?> message);

    void sendToGroup(String groupId, List<String> userIds, SseMessage<?> message);
}
