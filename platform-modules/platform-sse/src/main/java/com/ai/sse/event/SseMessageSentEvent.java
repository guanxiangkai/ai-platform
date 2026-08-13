package com.ai.sse.event;

import com.ai.sse.model.SseMessage;
import org.springframework.context.ApplicationEvent;

/**
 * SSE 消息发送事件
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public class SseMessageSentEvent extends ApplicationEvent {

    private final SseMessage<?> message;

    public SseMessageSentEvent(Object source, SseMessage<?> message) {
        super(source);
        this.message = message;
    }

    public SseMessage<?> getMessage() {
        return message;
    }
}
