package com.ai.sse.consumer.strategy;

import com.ai.api.sse.dto.SseNotification;
import com.ai.sse.service.ISseService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 多用户推送策略
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
public class UsersPushStrategy implements PushTargetStrategy {

    @Override
    public boolean supports(SseNotification.TargetType targetType) {
        return targetType == SseNotification.TargetType.USERS;
    }

    @Override
    public boolean checkOnline(ISseService sseService, SseNotification notification) {
        return !onlineUserIds(sseService, notification).isEmpty();
    }

    @Override
    public void push(ISseService sseService, SseNotification notification) {
        List<String> onlineUserIds = onlineUserIds(sseService, notification);
        if (!onlineUserIds.isEmpty()) {
            sseService.sendToUsers(onlineUserIds, notification.content());
        }
    }

    private List<String> onlineUserIds(ISseService sseService, SseNotification notification) {
        return notification.userIds() == null ? List.of() : notification.userIds().stream()
                .filter(sseService::isOnline)
                .toList();
    }
}
