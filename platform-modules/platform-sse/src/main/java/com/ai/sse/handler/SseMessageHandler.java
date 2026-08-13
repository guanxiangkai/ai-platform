package com.ai.sse.handler;

import com.ai.sse.model.SseMessage;

/**
 * SSE 消息处理器接口
 * <p>
 * 策略模式 — 每个实现声明自己支持的消息类型（字典值），
 * 由 {@link SseMessageDispatcher} 自动发现并注册。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@FunctionalInterface
public interface SseMessageHandler {

    /**
     * 处理消息
     */
    void handle(SseMessage<?> message);

    /**
     * 处理器名称（用于日志）
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 是否支持处理该消息
     */
    default boolean supports(SseMessage<?> message) {
        return true;
    }

    /**
     * 声明支持的消息类型（字典值字符串，返回 null 表示全局处理器）
     * <p>
     * Spring 自动发现时，根据此方法注册到 {@link SseMessageDispatcher}，
     * 无需在配置类中手动调用 {@code registerHandler}。
     * </p>
     */
    default String getSupportedType() {
        return null;
    }
}
