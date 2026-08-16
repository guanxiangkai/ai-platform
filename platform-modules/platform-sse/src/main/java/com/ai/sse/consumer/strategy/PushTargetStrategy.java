package com.ai.sse.consumer.strategy;

import com.ai.sse.model.mq.SseNotification;
import com.ai.sse.service.ISseService;

/**
 * 推送目标策略接口（策略模式）
 * <p>
 * 封装不同推送目标类型（USER / USERS / TENANT / BROADCAST）的
 * 在线检查与推送逻辑，消除消费者中的 switch 分支。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface PushTargetStrategy {

    /**
     * 是否支持该目标类型
     */
    boolean supports(SseNotification.TargetType targetType);

    /**
     * 检查目标是否在线
     *
     * @return true=有在线目标，允许推送
     */
    boolean checkOnline(ISseService sseService, SseNotification notification);

    /**
     * 执行推送
     */
    void push(ISseService sseService, SseNotification notification);
}
