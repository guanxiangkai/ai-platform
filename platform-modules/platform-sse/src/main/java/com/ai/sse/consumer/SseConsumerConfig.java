package com.ai.sse.consumer;

import com.ai.api.system.client.SystemClient;
import com.ai.api.system.dto.PushPreferenceBatchRequest;
import com.ai.api.system.dto.UserPushPreferenceDTO;
import com.ai.sse.config.SseProperties;
import io.github.guanxiangkai.web.plus.mq.model.MqMessage;
import io.github.guanxiangkai.web.plus.mq.model.SsePushLogMessage;
import io.github.guanxiangkai.web.plus.mq.producer.MessageProducer;
import com.ai.sse.constants.SseConstants;
import com.ai.sse.consumer.strategy.PushTargetStrategy;
import com.ai.api.sse.dto.SseNotification;
import com.ai.sse.service.ISseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * SSE 通知 MQ 消费者
 * <p>
 * 基于 Spring Cloud Stream 函数式编程模型，
 * 通过 {@code spring.cloud.stream.bindings.sseNotificationConsumer-in-0.destination=sse.notification} 绑定 topic。
 * </p>
 * <p>
 * 推送结果统一发布到 {@code log.sse-push} 主题，由 system 模块消费并持久化。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class SseConsumerConfig {

    private final ISseService sseService;
    private final MessageProducer messageProducer;
    private final SystemClient systemClient;
    private final SseProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<SseNotification.TargetType, PushTargetStrategy> strategyMap;

    public SseConsumerConfig(ISseService sseService,
                             MessageProducer messageProducer,
                             SystemClient systemClient,
                             SseProperties properties,
                             ObjectMapper objectMapper,
                             List<PushTargetStrategy> strategies) {
        this.sseService = sseService;
        this.messageProducer = messageProducer;
        this.systemClient = systemClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.strategyMap = strategies.stream()
                .flatMap(s -> java.util.Arrays.stream(SseNotification.TargetType.values())
                        .filter(s::supports)
                        .map(t -> Map.entry(t, s)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        log.info("[SSE-MQ] 已注册推送策略: {}", strategyMap.keySet());
    }

    /**
     * Spring Cloud Stream 函数式消费者
     * <p>
     * Bean 名称 = binding 名称前缀：sseNotificationConsumer-in-0
     * </p>
     */
    @Bean
    @SuppressWarnings("unchecked")
    public Consumer<Message<MqMessage>> sseNotificationConsumer() {
        return message -> {
            MqMessage raw = message.getPayload();
            SseNotification notification = convertPayload(raw.payload());

            if (notification == null) {
                log.warn("[SSE-MQ] payload 为空，丢弃消息: messageId={}", raw.messageId());
                return;
            }

            MqMessage<SseNotification> mqMessage = (MqMessage<SseNotification>) raw;

            // 查找推送策略
            PushTargetStrategy strategy = strategyMap.get(notification.targetType());
            if (strategy == null) {
                log.error("[SSE-MQ] 未知的推送目标类型: {}", notification.targetType());
                publishLog(mqMessage, notification, SseConstants.PushStatus.FAILED, "未知的推送目标类型: " + notification.targetType());
                return;
            }

            // 1. 在租户边界内批量求出允许推送的用户集合
            PreferenceResolution preferenceResolution = resolvePushPreferences(notification);
            if (!preferenceResolution.allowed()) {
                log.debug("[SSE-MQ] 推送偏好未通过，跳过: messageId={}, reason={}",
                        raw.messageId(), preferenceResolution.reason());
                publishLog(mqMessage, notification, SseConstants.PushStatus.SKIPPED,
                        preferenceResolution.reason());
                return;
            }
            notification = preferenceResolution.notification();

            // 2. 检查在线状态（委托给策略）
            if (!strategy.checkOnline(sseService, notification)) {
                String reason = "目标不在线: " + notification.targetType() + " - " +
                        (notification.userId() != null ? notification.userId() : notification.userIds());
                publishLog(mqMessage, notification, SseConstants.PushStatus.SKIPPED, reason);
                log.debug("[SSE-MQ] {}", reason);
                return;
            }

            // 3. 执行推送（委托给策略）
            try {
                log.debug("[SSE-MQ] 推送消息: messageId={}, targetType={}", raw.messageId(), notification.targetType());
                strategy.push(sseService, notification);
                publishLog(mqMessage, notification, SseConstants.PushStatus.SUCCESS, null);
            } catch (Exception e) {
                log.error("[SSE-MQ] 推送失败: messageId={}", raw.messageId(), e);
                publishLog(mqMessage, notification, SseConstants.PushStatus.FAILED, e.getMessage());
                throw e;
            }
        };
    }

    // ==================== 推送日志发布 ====================

    /**
     * 将推送结果异步发布到 log.sse-push 主题，由 system 模块落库到 sys_log
     */
    private void publishLog(MqMessage<SseNotification> message, SseNotification notification,
                            String status, String failReason) {
        try {
            SsePushLogMessage logMsg = new SsePushLogMessage(
                    message.messageId(),
                    notification.targetType() != null ? notification.targetType().name() : null,
                    notification.messageType(),
                    notification.userId(),
                    notification.userIds() != null ? String.join(",", notification.userIds()) : null,
                    notification.content() != null ? notification.content().toString() : null,
                    status,
                    failReason,
                    0,
                    null,
                    LocalDateTime.now()
            );
            messageProducer.sendAsync(SsePushLogMessage.TOPIC, logMsg);
        } catch (Exception e) {
            log.warn("[SSE-MQ] 推送日志发布失败（不影响主流程）: messageId={}, error={}",
                    message.messageId(), e.getMessage());
        }
    }

    // ==================== Payload 转换 ====================

    /**
     * 将 MQ 反序列化的 payload（可能是 LinkedHashMap）转换为 SseNotification
     */
    private SseNotification convertPayload(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof SseNotification notification) {
            return notification;
        }
        try {
            return objectMapper.convertValue(payload, SseNotification.class);
        } catch (Exception e) {
            log.error("[SSE-MQ] payload 转换失败: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 用户推送偏好 ====================

    private PreferenceResolution resolvePushPreferences(SseNotification notification) {
        if (notification.targetType() != SseNotification.TargetType.USER
                && notification.targetType() != SseNotification.TargetType.USERS) {
            return PreferenceResolution.allowed(notification);
        }
        if (notification.tenantId() == null || notification.tenantId().isBlank()) {
            return PreferenceResolution.skipped("用户推送通知缺少租户标识");
        }

        PushPreferenceBatchRequest request;
        try {
            request = new PushPreferenceBatchRequest(targetUserIds(notification));
        } catch (IllegalArgumentException exception) {
            return PreferenceResolution.skipped(exception.getMessage());
        }
        if (request.userIds().isEmpty()) {
            return PreferenceResolution.skipped("用户推送通知缺少有效的目标用户");
        }

        try {
            List<UserPushPreferenceDTO> preferences = systemClient
                    .getPushPreferencesForTenant(notification.tenantId(), request)
                    .timeout(properties.pushPreferenceQueryTimeout())
                    .block();
            List<String> allowedUserIds = allowedUserIds(request.userIds(), preferences);
            if (allowedUserIds.isEmpty()) {
                return PreferenceResolution.skipped("所有目标用户均已关闭推送通知");
            }
            return PreferenceResolution.allowed(withAllowedUsers(notification, allowedUserIds));
        } catch (Exception e) {
            return resolvePreferenceFailure(notification, e);
        }
    }

    private PreferenceResolution resolvePreferenceFailure(
            SseNotification notification, Exception exception) {
        if (properties.pushPreferenceFailurePolicy() == SseProperties.PushPreferenceFailurePolicy.RETRY) {
            log.error("[SSE-MQ] 推送偏好查询失败，按 RETRY 策略交给消息中间件重试: messageType={}, error={}",
                    notification.messageType(), exception.getMessage());
            throw new IllegalStateException("推送偏好查询失败，等待消息重试", exception);
        }
        log.warn("[SSE-MQ] 推送偏好查询失败，按 FAIL_CLOSED 策略跳过: messageType={}, error={}",
                notification.messageType(), exception.getMessage());
        return PreferenceResolution.skipped("推送偏好查询失败，已按 FAIL_CLOSED 策略跳过");
    }

    private List<String> targetUserIds(SseNotification notification) {
        return notification.targetType() == SseNotification.TargetType.USER
                ? List.of(notification.userId() == null ? "" : notification.userId())
                : notification.userIds();
    }

    private List<String> allowedUserIds(
            List<String> requestedUserIds, List<UserPushPreferenceDTO> preferences) {
        Map<String, Boolean> preferenceByUserId = new LinkedHashMap<>();
        if (preferences != null) {
            preferences.stream()
                    .filter(preference -> preference != null && preference.userId() != null)
                    .forEach(preference -> preferenceByUserId.put(
                            preference.userId(), preference.pushEnabled()));
        }
        return requestedUserIds.stream()
                .filter(userId -> Boolean.TRUE.equals(preferenceByUserId.get(userId)))
                .toList();
    }

    private SseNotification withAllowedUsers(SseNotification notification, List<String> allowedUserIds) {
        if (notification.targetType() == SseNotification.TargetType.USER) {
            return new SseNotification(
                    SseNotification.TargetType.USER,
                    allowedUserIds.getFirst(),
                    null,
                    notification.tenantId(),
                    notification.messageType(),
                    notification.content()
            );
        }
        return new SseNotification(
                SseNotification.TargetType.USERS,
                null,
                allowedUserIds,
                notification.tenantId(),
                notification.messageType(),
                notification.content()
        );
    }

    private record PreferenceResolution(
            boolean allowed, SseNotification notification, String reason) {

        private static PreferenceResolution allowed(SseNotification notification) {
            return new PreferenceResolution(true, notification, null);
        }

        private static PreferenceResolution skipped(String reason) {
            return new PreferenceResolution(false, null, reason);
        }
    }
}
