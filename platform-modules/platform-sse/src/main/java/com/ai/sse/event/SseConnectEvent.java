package com.ai.sse.event;

import com.ai.sse.model.SseConnection;
import org.springframework.context.ApplicationEvent;

/**
 * SSE 连接建立事件
 * <p>
 * 通过 Spring {@code @EventListener} 按观察者模式订阅。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public class SseConnectEvent extends ApplicationEvent {

    private final SseConnection connection;

    public SseConnectEvent(Object source, SseConnection connection) {
        super(source);
        this.connection = connection;
    }

    public SseConnection getConnection() {
        return connection;
    }
}
