package com.ai.sse.service.impl;

import com.ai.sse.core.RedisSseMessageBridge;
import com.ai.sse.core.SseOperations;
import com.ai.sse.handler.SseMessageDispatcher;
import com.ai.sse.model.SseMessage;
import com.ai.sse.model.mq.SseNotification.TargetType;
import com.ai.sse.service.ISseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * SSE 服务门面实现
 * <p>
 * 将 {@link SseOperations}（连接 + 推送）与 {@link SseMessageDispatcher}（消息调度）
 * 统一封装，对外暴露简洁 API。
 * </p>
 * <p>
 * 多实例路由：当配置了 {@link RedisSseMessageBridge} 时，
 * 消息推送会通过 Redis PubSub 广播到所有实例，确保用户无论连接在哪个节点都能收到消息。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseServiceImpl implements ISseService {


    private final SseOperations sseOperations;
    private final SseMessageDispatcher dispatcher;
    private final RedisSseMessageBridge messageBridge;

    // ==================== 连接管理 ====================

    @Override
    public Flux<ServerSentEvent<String>> connect(String userId, String tenantId) {
        return sseOperations.connect(userId, tenantId);
    }

    @Override
    public void disconnect(String userId) {
        sseOperations.disconnect(userId);
    }

    @Override
    public boolean isOnline(String userId) {
        return sseOperations.isOnline(userId);
    }

    @Override
    public int getOnlineCount() {
        return sseOperations.getOnlineCount();
    }

    @Override
    public int getOnlineCountByTenant(String tenantId) {
        return sseOperations.getOnlineCountByTenant(tenantId);
    }

    // ==================== 消息推送（本地 + PubSub） ====================

    @Override
    public void sendToUser(String userId, String messageType, Object content) {
        SseMessage<Object> message = SseMessage.builder()
                .type(messageType)
                .content(content)
                .build();
        sendToUser(userId, message);
    }

    @Override
    public void sendToUser(String userId, SseMessage<?> message) {
        // 本地投递
        sseOperations.sendToUser(userId, message);
        // PubSub 广播（其他实例也尝试投递）
        publishIfEnabled(TargetType.USER, userId, null, null, message);
    }

    @Override
    public void sendToUsers(List<String> userIds, Object content) {
        SseMessage<Object> message = SseMessage.notification(content);
        // 本地投递
        sseOperations.sendToUsers(userIds, message);
        // PubSub 广播
        publishIfEnabled(TargetType.USERS, null, userIds, null, message);
    }

    @Override
    public void broadcast(Object content) {
        SseMessage<Object> message = SseMessage.notification(content);
        // 本地投递
        sseOperations.broadcast(message);
        // PubSub 广播
        publishIfEnabled(TargetType.BROADCAST, null, null, null, message);
    }

    @Override
    public void broadcastToTenant(String tenantId, Object content) {
        SseMessage<Object> message = SseMessage.notification(content);
        // 本地投递
        sseOperations.broadcastToTenant(tenantId, message);
        // PubSub 广播
        publishIfEnabled(TargetType.TENANT, null, null, tenantId, message);
    }

    // ==================== 消息调度 ====================

    @Override
    public void dispatchMessage(SseMessage<?> message) {
        dispatcher.dispatch(message);
    }

    // ==================== 私有方法 ====================

    /**
     * 如果启用了 PubSub，则发布消息到 Redis
     */
    private void publishIfEnabled(TargetType targetType, String userId, List<String> userIds,
                                  String tenantId, SseMessage<?> message) {
        if (messageBridge != null) {
            messageBridge.publish(targetType, userId, userIds, tenantId, message);
        }
    }
}
