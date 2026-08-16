package com.ai.sse.core;

import com.ai.sse.config.SseProperties;
import com.ai.sse.model.SseMessage;
import com.ai.sse.model.mq.SseNotification.TargetType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Redis PubSub SSE 消息桥（多实例路由）
 * <p>
 * 解决 SSE 集群部署时的消息路由问题：
 * 用户的 SSE 连接可能在任意实例上，推送消息时需要广播到所有实例。
 * </p>
 * <p>
 * 工作原理：
 * <ol>
 *   <li>推送消息时，先尝试本地投递</li>
 *   <li>无论是否成功，都通过 Redis PubSub 广播到所有实例</li>
 *   <li>其他实例收到后尝试本地投递（通过 instanceId 避免重复投递）</li>
 * </ol>
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
public class RedisSseMessageBridge implements MessageListener {

    /**
     * 当前实例唯一标识（用于去重）
     */
    @Getter
    private final String instanceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    private final SseOperations sseOperations;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    @Getter
    private final String channel;

    public RedisSseMessageBridge(SseOperations sseOperations,
                                 StringRedisTemplate redisTemplate,
                                 ObjectMapper objectMapper,
                                 SseProperties sseProperties) {
        this.sseOperations = sseOperations;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.channel = sseProperties.pubsubChannel();
        log.info("[SSE-PubSub] 消息桥初始化: instanceId={}, channel={}", instanceId, channel);
    }

    // ==================== 发布（推送端调用） ====================

    /**
     * 发布消息到 Redis PubSub（广播到所有实例）
     *
     * @param targetType 推送目标类型
     * @param userId     单用户 ID（USER 类型时）
     * @param userIds    多用户 ID（USERS 类型时）
     * @param tenantId   租户 ID（TENANT 类型时）
     * @param message    SSE 消息体
     */
    public void publish(TargetType targetType, String userId, List<String> userIds,
                        String tenantId, SseMessage<?> message) {
        try {
            PubSubPayload payload = new PubSubPayload(instanceId, targetType, userId, userIds, tenantId, message);
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(channel, json);
            log.debug("[SSE-PubSub] 消息已发布: targetType={}, instanceId={}", targetType, instanceId);
        } catch (Exception e) {
            log.error("[SSE-PubSub] 发布消息失败: targetType={}, exception={}",
                    targetType, e.getClass().getSimpleName());
        }
    }

    // ==================== 订阅（接收端回调） ====================

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            PubSubPayload payload = objectMapper.readValue(json, PubSubPayload.class);

            // 跳过本实例发出的消息（已在本地投递过）
            if (instanceId.equals(payload.sourceInstanceId())) {
                return;
            }

            log.debug("[SSE-PubSub] 收到跨实例消息: targetType={}, from={}",
                    payload.targetType(), payload.sourceInstanceId());

            // 本地投递
            deliverLocally(payload);
        } catch (Exception e) {
            log.error("[SSE-PubSub] 处理订阅消息失败: exception={}",
                    e.getClass().getSimpleName());
        }
    }

    /**
     * 本地投递消息
     */
    private void deliverLocally(PubSubPayload payload) {
        SseMessage<?> message = payload.message();
        switch (payload.targetType()) {
            case USER -> {
                if (sseOperations.isOnline(payload.userId())) {
                    sseOperations.sendToUser(payload.userId(), message);
                }
            }
            case USERS -> {
                List<String> ids = payload.userIds();
                ids.stream()
                        .filter(sseOperations::isOnline)
                        .forEach(uid -> sseOperations.sendToUser(uid, message));
            }
            case TENANT -> sseOperations.broadcastToTenant(payload.tenantId(), message);
            case BROADCAST -> sseOperations.broadcast(message);
        }
    }


    // ==================== PubSub 载荷 ====================

    /**
     * Redis PubSub 消息载荷
     */
    public record PubSubPayload(
            String sourceInstanceId,
            TargetType targetType,
            String userId,
            List<String> userIds,
            String tenantId,
            SseMessage<?> message
    ) {
    }
}
