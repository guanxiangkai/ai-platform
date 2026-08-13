package com.ai.api.sse.dto;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import java.io.Serializable;
import java.util.List;

/**
 * 平台 SSE 服务接收的跨服务通知契约。
 *
 * <p>业务服务只依赖该公共 DTO 并通过 {@code sse.notification} 消息主题发送通知，
 * 连接管理、在线判断和最终推送仍由平台 SSE 服务负责。</p>
 *
 * @param targetType 推送目标范围
 * @param userId 单用户标识，仅 {@link TargetType#USER} 使用
 * @param userIds 多用户标识，仅 {@link TargetType#USERS} 使用
 * @param tenantId 租户标识，用户与租户推送时必填
 * @param messageType 业务消息类型
 * @param content 业务消息内容
 */
@RegisterReflectionForBinding
public record SseNotification(
        TargetType targetType,
        String userId,
        List<String> userIds,
        String tenantId,
        String messageType,
        Object content
) implements Serializable {

    /** 创建单用户通知。 */
    public static SseNotification toUser(String tenantId, String userId, String messageType, Object content) {
        return new SseNotification(TargetType.USER, userId, null, tenantId, messageType, content);
    }

    /** 创建多用户通知。 */
    public static SseNotification toUsers(
            String tenantId, List<String> userIds, String messageType, Object content) {
        return new SseNotification(TargetType.USERS, null, List.copyOf(userIds), tenantId, messageType, content);
    }

    /** 创建租户广播通知。 */
    public static SseNotification toTenant(String tenantId, String messageType, Object content) {
        return new SseNotification(TargetType.TENANT, null, null, tenantId, messageType, content);
    }

    /** 创建全平台广播通知。 */
    public static SseNotification broadcast(String messageType, Object content) {
        return new SseNotification(TargetType.BROADCAST, null, null, null, messageType, content);
    }

    /** 平台支持的推送目标范围。 */
    @RegisterReflectionForBinding
    public enum TargetType {
        USER,
        USERS,
        TENANT,
        BROADCAST
    }
}
