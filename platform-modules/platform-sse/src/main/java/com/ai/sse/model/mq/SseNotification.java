package com.ai.sse.model.mq;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import java.util.List;

/**
 * SSE 通知 MQ 消息体
 * <p>
 * 由业务服务发送到 MQ，ai-sse 消费后推送给在线 SSE 客户端。
 * {@code targetType} 决定推送范围，策略模式处理各类型差异。
 * {@code messageType} 为字典值字符串（对应字典 {@code sse_message_type}）。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RegisterReflectionForBinding
public record SseNotification(
        TargetType targetType,
        String userId,
        List<String> userIds,
        String tenantId,
        String messageType,
        Object content
) {

    // ==================== 推送目标类型 ====================

    public static SseNotification toUser(String tenantId, String userId, String messageType, Object content) {
        return new SseNotification(TargetType.USER, userId, null, tenantId, messageType, content);
    }

    // ==================== 工厂方法 ====================

    public static SseNotification toUsers(
            String tenantId, List<String> userIds, String messageType, Object content) {
        return new SseNotification(TargetType.USERS, null, userIds, tenantId, messageType, content);
    }

    public static SseNotification toTenant(String tenantId, String messageType, Object content) {
        return new SseNotification(TargetType.TENANT, null, null, tenantId, messageType, content);
    }

    public static SseNotification broadcast(String messageType, Object content) {
        return new SseNotification(TargetType.BROADCAST, null, null, null, messageType, content);
    }

    @RegisterReflectionForBinding
    public enum TargetType {
        /**
         * 单用户推送
         */
        USER,
        /**
         * 多用户推送
         */
        USERS,
        /**
         * 租户广播
         */
        TENANT,
        /**
         * 全局广播
         */
        BROADCAST
    }
}
