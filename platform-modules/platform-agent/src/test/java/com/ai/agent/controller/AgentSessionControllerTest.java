package com.ai.agent.controller;

import com.ai.agent.domain.dto.AgentSessionAskRequest;
import com.ai.agent.service.AgentSessionService;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentSessionControllerTest {

    @Test
    void streamDoesNotExposeUnexpectedExceptionDetails() {
        AgentSessionService sessions = mock(AgentSessionService.class);
        AgentSessionAskRequest request = mock(AgentSessionAskRequest.class);
        when(sessions.stream(request)).thenThrow(new IllegalStateException(
                "https://provider.example.invalid/?token=fake-private-detail"));

        var events = new AgentSessionController(sessions).stream(request)
                .collectList().block(Duration.ofSeconds(2));

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().data().message()).isEqualTo("智能体会话暂不可用，请稍后重试");
    }

    @Test
    void streamPreservesLocalBusinessValidationMessage() {
        AgentSessionService sessions = mock(AgentSessionService.class);
        AgentSessionAskRequest request = mock(AgentSessionAskRequest.class);
        when(sessions.stream(request)).thenThrow(new BizException("智能体已停用"));

        var events = new AgentSessionController(sessions).stream(request)
                .collectList().block(Duration.ofSeconds(2));

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().data().message()).isEqualTo("智能体已停用");
    }
}
