package com.ai.sse.consumer.strategy;

import com.ai.sse.model.mq.SseNotification;
import com.ai.sse.service.ISseService;
import org.springframework.stereotype.Component;

/**
 * 单用户推送策略
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
public class UserPushStrategy implements PushTargetStrategy {

    @Override
    public boolean supports(SseNotification.TargetType targetType) {
        return targetType == SseNotification.TargetType.USER;
    }

    @Override
    public boolean checkOnline(ISseService sseService, SseNotification notification) {
        return sseService.isOnline(notification.userId());
    }

    @Override
    public void push(ISseService sseService, SseNotification notification) {
        sseService.sendToUser(notification.userId(), notification.messageType(), notification.content());
    }
}
