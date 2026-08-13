package com.ai.sse.constants;

/**
 * SSE 模块常量
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class SseConstants {

    private SseConstants() {
        throw new UnsupportedOperationException("这是一个效用类，无法实例化");
    }

    /**
     * 推送状态常量（字典：sse_push_status）
     */
    public static final class PushStatus {

        /**
         * 待推送
         */
        public static final String PENDING = "pending";

        /**
         * 推送成功
         */
        public static final String SUCCESS = "success";

        /**
         * 已跳过（如目标用户不在线）
         */
        public static final String SKIPPED = "skipped";

        /**
         * 推送失败
         */
        public static final String FAILED = "failed";

        private PushStatus() {
        }
    }

    /**
     * 消息类型常量（字典：sse_message_type）
     */
    public static final class MessageType {

        public static final String CONNECT = "connect";
        public static final String DISCONNECT = "disconnect";
        public static final String HEARTBEAT = "heartbeat";
        public static final String SYSTEM = "system";
        public static final String NOTIFICATION = "notification";
        public static final String GROUP = "group";
        public static final String BUSINESS = "business";

        private MessageType() {
        }
    }

    /**
     * 连接状态常量（字典：sse_connection_status）
     */
    public static final class ConnectionStatus {

        /**
         * 已连接
         */
        public static final String CONNECTED = "connected";

        /**
         * 已断开
         */
        public static final String DISCONNECTED = "disconnected";

        /**
         * 连接异常
         */
        public static final String ERROR = "error";

        /**
         * 连接超时
         */
        public static final String TIMEOUT = "timeout";

        private ConnectionStatus() {
        }
    }

    /**
     * Topic 常量
     */
    public static final class TopicConstants {

        public static final String NOTIFICATION = "sse.notification";

        private TopicConstants() {
        }
    }

    /**
     * Redis key 常量
     */
    public static final class RedisKeyConstants {

        public static final String TICKET_PREFIX = "sse:ticket:";
        public static final String TICKET_RATE_LIMIT_PREFIX = "sse:rate:ticket:";

        private RedisKeyConstants() {
        }
    }

    /**
     * 票据常量
     */
    public static final class TicketConstants {

        public static final String VALUE_SEPARATOR = "|";

        private TicketConstants() {
        }
    }

}
