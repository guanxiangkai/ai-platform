package com.ai.sse.event;

import com.ai.sse.model.SseConnection;
import org.springframework.context.ApplicationEvent;

/**
 * SSE 连接断开事件
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public class SseDisconnectEvent extends ApplicationEvent {

    private final SseConnection connection;

    public SseDisconnectEvent(Object source, SseConnection connection) {
        super(source);
        this.connection = connection;
    }

    public SseConnection getConnection() {
        return connection;
    }
}
