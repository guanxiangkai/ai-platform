package com.ai.sse.consumer.strategy;

import com.ai.sse.model.mq.SseNotification;
import com.ai.sse.service.ISseService;
import org.springframework.stereotype.Component;

/**
 * 全局广播推送策略
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
public class BroadcastPushStrategy implements PushTargetStrategy {

    @Override
    public boolean supports(SseNotification.TargetType targetType) {
        return targetType == SseNotification.TargetType.BROADCAST;
    }

    /**
     * 广播无需在线检查
     */
    @Override
    public boolean checkOnline(ISseService sseService, SseNotification notification) {
        return true;
    }

    @Override
    public void push(ISseService sseService, SseNotification notification) {
        sseService.broadcast(notification.content());
    }
}
