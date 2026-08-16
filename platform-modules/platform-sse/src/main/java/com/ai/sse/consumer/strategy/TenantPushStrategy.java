package com.ai.sse.consumer.strategy;

import com.ai.sse.model.mq.SseNotification;
import com.ai.sse.service.ISseService;
import org.springframework.stereotype.Component;

/**
 * 租户广播推送策略
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
public class TenantPushStrategy implements PushTargetStrategy {

    @Override
    public boolean supports(SseNotification.TargetType targetType) {
        return targetType == SseNotification.TargetType.TENANT;
    }

    /**
     * 租户广播无法提前判断是否有人在线，默认允许推送
     */
    @Override
    public boolean checkOnline(ISseService sseService, SseNotification notification) {
        return true;
    }

    @Override
    public void push(ISseService sseService, SseNotification notification) {
        sseService.broadcastToTenant(notification.tenantId(), notification.content());
    }
}
