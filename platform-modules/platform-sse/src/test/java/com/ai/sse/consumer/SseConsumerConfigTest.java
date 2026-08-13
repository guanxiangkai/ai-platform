package com.ai.sse.consumer;

import com.ai.api.system.client.SystemClient;
import com.ai.api.system.dto.PushPreferenceBatchRequest;
import com.ai.api.system.dto.UserPushPreferenceDTO;
import com.ai.sse.config.SseProperties;
import com.ai.sse.consumer.strategy.BroadcastPushStrategy;
import com.ai.sse.consumer.strategy.TenantPushStrategy;
import com.ai.sse.consumer.strategy.UserPushStrategy;
import com.ai.sse.consumer.strategy.UsersPushStrategy;
import com.ai.api.sse.dto.SseNotification;
import com.ai.sse.service.ISseService;
import io.github.guanxiangkai.web.plus.mq.model.MqMessage;
import io.github.guanxiangkai.web.plus.mq.producer.MessageProducer;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SseConsumerConfigTest {

    @Test
    void usersPushShouldSendOnlyToEnabledUsersWithOneBatchQuery() {
        Fixture fixture = fixture(SseProperties.PushPreferenceFailurePolicy.FAIL_CLOSED);
        Object content = "notice";
        SseNotification notification = SseNotification.toUsers(
                "tenant-1", List.of("enabled", "disabled"), "notice", content);
        when(fixture.systemClient().getPushPreferencesForTenant(eq("tenant-1"), any()))
                .thenReturn(Mono.just(List.of(
                        new UserPushPreferenceDTO("enabled", true),
                        new UserPushPreferenceDTO("disabled", false)
                )));
        when(fixture.sseService().isOnline("enabled")).thenReturn(true);

        fixture.consumer().accept(message(notification));

        verify(fixture.systemClient()).getPushPreferencesForTenant(
                "tenant-1", new PushPreferenceBatchRequest(List.of("enabled", "disabled")));
        verify(fixture.sseService()).sendToUsers(List.of("enabled"), content);
        verify(fixture.sseService(), never()).sendToUsers(List.of("enabled", "disabled"), content);
    }

    @Test
    void missingPreferenceResultShouldFailClosedForThatUser() {
        Fixture fixture = fixture(SseProperties.PushPreferenceFailurePolicy.FAIL_CLOSED);
        SseNotification notification = SseNotification.toUsers(
                "tenant-1", List.of("known", "missing"), "notice", "content");
        when(fixture.systemClient().getPushPreferencesForTenant(eq("tenant-1"), any()))
                .thenReturn(Mono.just(List.of(new UserPushPreferenceDTO("known", true))));
        when(fixture.sseService().isOnline("known")).thenReturn(true);

        fixture.consumer().accept(message(notification));

        verify(fixture.sseService()).sendToUsers(List.of("known"), "content");
    }

    @Test
    void preferenceFailureShouldSkipWhenFailClosed() {
        Fixture fixture = fixture(SseProperties.PushPreferenceFailurePolicy.FAIL_CLOSED);
        SseNotification notification = SseNotification.toUser(
                "tenant-1", "user-1", "notice", "content");
        when(fixture.systemClient().getPushPreferencesForTenant(eq("tenant-1"), any()))
                .thenReturn(Mono.error(new IllegalStateException("system unavailable")));

        fixture.consumer().accept(message(notification));

        verify(fixture.sseService(), never()).sendToUser(any(), any(), any());
    }

    @Test
    void preferenceFailureShouldPropagateWhenRetryPolicyIsConfigured() {
        Fixture fixture = fixture(SseProperties.PushPreferenceFailurePolicy.RETRY);
        SseNotification notification = SseNotification.toUser(
                "tenant-1", "user-1", "notice", "content");
        when(fixture.systemClient().getPushPreferencesForTenant(eq("tenant-1"), any()))
                .thenReturn(Mono.error(new IllegalStateException("system unavailable")));

        assertThatThrownBy(() -> fixture.consumer().accept(message(notification)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("推送偏好查询失败，等待消息重试")
                .hasCauseInstanceOf(IllegalStateException.class);
        verify(fixture.sseService(), never()).sendToUser(any(), any(), any());
    }

    @Test
    void preferenceQueryTimeoutShouldApplyConfiguredFailClosedPolicy() {
        Fixture fixture = fixture(
                SseProperties.PushPreferenceFailurePolicy.FAIL_CLOSED, Duration.ofMillis(1));
        SseNotification notification = SseNotification.toUser(
                "tenant-1", "user-1", "notice", "content");
        when(fixture.systemClient().getPushPreferencesForTenant(eq("tenant-1"), any()))
                .thenReturn(Mono.never());

        fixture.consumer().accept(message(notification));

        verify(fixture.sseService(), never()).sendToUser(any(), any(), any());
    }

    @Test
    void notificationWithoutTenantShouldNeverQueryOrPushUserTargets() {
        Fixture fixture = fixture(SseProperties.PushPreferenceFailurePolicy.RETRY);
        SseNotification notification = new SseNotification(
                SseNotification.TargetType.USER, "user-1", null, null, "notice", "content");

        fixture.consumer().accept(message(notification));

        verify(fixture.systemClient(), never()).getPushPreferencesForTenant(any(), any());
        verify(fixture.sseService(), never()).sendToUser(any(), any(), any());
    }

    private Fixture fixture(SseProperties.PushPreferenceFailurePolicy failurePolicy) {
        return fixture(failurePolicy, Duration.ofMillis(50));
    }

    private Fixture fixture(
            SseProperties.PushPreferenceFailurePolicy failurePolicy, Duration queryTimeout) {
        ISseService sseService = mock(ISseService.class);
        SystemClient systemClient = mock(SystemClient.class);
        MessageProducer messageProducer = mock(MessageProducer.class);
        SseProperties properties = new SseProperties(
                null, null, null, null, null, null, null, null, null, null,
                null, queryTimeout, failurePolicy, null);
        SseConsumerConfig configuration = new SseConsumerConfig(
                sseService,
                messageProducer,
                systemClient,
                properties,
                mock(ObjectMapper.class),
                List.of(
                        new UserPushStrategy(),
                        new UsersPushStrategy(),
                        new TenantPushStrategy(),
                        new BroadcastPushStrategy()
                )
        );
        return new Fixture(sseService, systemClient, configuration.sseNotificationConsumer());
    }

    private Message<MqMessage> message(SseNotification notification) {
        return MessageBuilder.withPayload(
                (MqMessage) MqMessage.of("message-1", "sse.notification", notification)
        ).build();
    }

    private record Fixture(
            ISseService sseService,
            SystemClient systemClient,
            Consumer<Message<MqMessage>> consumer) {
    }
}
