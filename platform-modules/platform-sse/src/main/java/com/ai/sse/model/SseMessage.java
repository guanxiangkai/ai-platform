package com.ai.sse.model;

import com.ai.sse.constants.SseConstants;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SSE 消息模型（不可变）
 * <p>
 * 泛型 {@code T} 表示消息体类型，支持任意业务 payload。
 * {@code type} 为字典值字符串（对应字典 {@code sse_message_type}），
 * 不再使用枚举，由 web-plus-dict 统一管理。
 * </p>
 *
 * @param <T> 消息内容类型
 * @author guanxiangkai
 * @since 1.0.0
 */
@RegisterReflectionForBinding
public record SseMessage<T>(
        String id,
        String type,
        String senderId,
        String senderName,
        String groupId,
        T content,
        LocalDateTime sendTime,
        String tenantId
) {

    /**
     * 紧凑型构造器 — 填充默认值
     */
    public SseMessage {
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "");
        }
        if (sendTime == null) {
            sendTime = LocalDateTime.now();
        }
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 连接成功消息
     */
    public static SseMessage<String> connect(String userId) {
        return SseMessage.<String>builder()
                .type(SseConstants.MessageType.CONNECT)
                .senderId(userId)
                .content("connected")
                .build();
    }

    /**
     * 心跳响应消息
     */
    public static SseMessage<String> heartbeat(String userId) {
        return SseMessage.<String>builder()
                .type(SseConstants.MessageType.HEARTBEAT)
                .senderId(userId)
                .content("pong")
                .build();
    }

    /**
     * 系统通知
     */
    public static <T> SseMessage<T> system(T content) {
        return SseMessage.<T>builder()
                .type(SseConstants.MessageType.SYSTEM)
                .content(content)
                .build();
    }

    /**
     * 普通通知
     */
    public static <T> SseMessage<T> notification(T content) {
        return SseMessage.<T>builder()
                .type(SseConstants.MessageType.NOTIFICATION)
                .content(content)
                .build();
    }

    /**
     * 自定义类型消息
     */
    public static <T> SseMessage<T> of(String type, T content) {
        return SseMessage.<T>builder()
                .type(type)
                .content(content)
                .build();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    // ==================== Builder ====================

    @RegisterReflectionForBinding
    public static final class Builder<T> {
        private String id;
        private String type;
        private String senderId;
        private String senderName;
        private String groupId;
        private T content;
        private LocalDateTime sendTime;
        private String tenantId;

        private Builder() {
        }

        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        public Builder<T> type(String type) {
            this.type = type;
            return this;
        }

        public Builder<T> senderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder<T> senderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public Builder<T> groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder<T> content(T content) {
            this.content = content;
            return this;
        }

        public Builder<T> sendTime(LocalDateTime sendTime) {
            this.sendTime = sendTime;
            return this;
        }

        public Builder<T> tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public SseMessage<T> build() {
            return new SseMessage<>(id, type, senderId, senderName, groupId, content, sendTime, tenantId);
        }
    }
}
