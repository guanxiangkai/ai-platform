package com.ai.sse.domain;

/**
 * SSE 操作类型。
 *
 * @since 1.0.0
 */
public enum SseOperationType {
    /** 建立连接。 */
    CONNECT,
    /** 断开连接。 */
    DISCONNECT,
    /** 推送消息。 */
    PUSH
}
