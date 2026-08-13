package com.ai.system.service.impl;

import com.ai.system.repository.MessageRepository;
import io.github.guanxiangkai.web.plus.mq.producer.MessageProducer;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MessageServiceImplSseContractTest {

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void userNotificationShouldCarryTrustedCurrentTenant() {
        UserContextHolder.set(new UserContext(
                "sender-1",
                "tenant-1",
                false,
                "dept-1",
                Set.of("dept-1"),
                Set.of("OPERATOR"),
                Set.of(),
                Map.of()
        ));
        MessageServiceImpl service = new MessageServiceImpl(
                mock(MessageRepository.class),
                mock(MessageProducer.class),
                mock(Converter.class)
        );

        Map<String, Object> notification = ReflectionTestUtils.invokeMethod(
                service, "buildUserNotification", "receiver-1", "notice", "content");

        assertThat(notification)
                .containsEntry("targetType", "USER")
                .containsEntry("userId", "receiver-1")
                .containsEntry("tenantId", "tenant-1");
    }
}
