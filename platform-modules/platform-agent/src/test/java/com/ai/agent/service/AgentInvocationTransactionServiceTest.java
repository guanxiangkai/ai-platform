package com.ai.agent.service;

import com.ai.agent.config.AgentInvocationProperties;
import com.ai.agent.domain.AgentInvocationMode;
import com.ai.agent.domain.AgentInvocationState;
import com.ai.agent.domain.AgentProviderType;
import com.ai.agent.domain.AgentSessionState;
import com.ai.agent.domain.dto.AgentInvocationRequest;
import com.ai.agent.domain.entity.AgentCallRecord;
import com.ai.agent.domain.entity.AgentConfig;
import com.ai.agent.domain.entity.AgentMessageRecord;
import com.ai.agent.domain.entity.AgentSessionRecord;
import com.ai.agent.repository.AgentCallRecordRepository;
import com.ai.agent.repository.AgentMessageRecordRepository;
import com.ai.agent.repository.AgentSessionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentInvocationTransactionServiceTest {
    private AgentSessionRecordRepository sessions;
    private AgentMessageRecordRepository messages;
    private AgentCallRecordRepository calls;
    private AgentInvocationTransactionService service;

    @BeforeEach
    void setUp() {
        sessions = mock(AgentSessionRecordRepository.class);
        messages = mock(AgentMessageRecordRepository.class);
        calls = mock(AgentCallRecordRepository.class);
        service = new AgentInvocationTransactionService(
                sessions, messages, calls, new AgentInvocationProperties());
    }

    @Test
    void reservationShouldAllocateTwoUniqueSequenceSlotsAndPersistUserMessageBeforeProviderCall() {
        AgentSessionRecord session = session();
        when(calls.findLockedByTenantIdAndInvocationId("tenant-1", "invocation-1"))
                .thenReturn(Optional.empty());
        when(sessions.findLockedByIdAndTenantId("session-1", "tenant-1"))
                .thenReturn(Optional.of(session));
        when(messages.findBySessionIdAndTenantIdOrderBySequenceNoAsc("session-1", "tenant-1"))
                .thenReturn(List.of());
        when(messages.saveAndFlush(any())).thenAnswer(invocation -> {
            AgentMessageRecord message = invocation.getArgument(0);
            message.setId("user-message-1");
            return message;
        });
        when(calls.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var reservation = service.reserve(
                definition(), request(), "tenant-1", "user-1", "invocation-1", "fingerprint-1");

        assertThat(reservation.action())
                .isEqualTo(AgentInvocationTransactionService.ReservationAction.EXECUTE_PROVIDER);
        assertThat(session.getActiveInvocationId()).isEqualTo("invocation-1");
        assertThat(session.getMessageCount()).isEqualTo(2);
        ArgumentCaptor<AgentMessageRecord> message = ArgumentCaptor.forClass(AgentMessageRecord.class);
        verify(messages).saveAndFlush(message.capture());
        assertThat(message.getValue().getSequenceNo()).isEqualTo(1);
        assertThat(message.getValue().getInvocationId()).isEqualTo("invocation-1");
        ArgumentCaptor<AgentCallRecord> call = ArgumentCaptor.forClass(AgentCallRecord.class);
        verify(calls).saveAndFlush(call.capture());
        assertThat(call.getValue().getReservedUserSequenceNo()).isEqualTo(1);
        assertThat(call.getValue().getReservedAssistantSequenceNo()).isEqualTo(2);
    }

    @Test
    void differentInvocationShouldNotEnterSessionWhileAnotherInvocationIsActive() {
        AgentSessionRecord session = session();
        session.setActiveInvocationId("invocation-active");
        when(calls.findLockedByTenantIdAndInvocationId("tenant-1", "invocation-2"))
                .thenReturn(Optional.empty());
        when(sessions.findLockedByIdAndTenantId("session-1", "tenant-1"))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.reserve(
                definition(), request(), "tenant-1", "user-1", "invocation-2", "fingerprint-2"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("当前会话存在进行中的调用");
        verify(messages, never()).saveAndFlush(any());
    }

    @Test
    void reusedInvocationIdShouldRejectDifferentRequestFingerprint() {
        AgentCallRecord existing = call(AgentInvocationState.SUCCEEDED);
        existing.setAgentId("agent-1");
        existing.setRequestFingerprint("fingerprint-original");
        when(calls.findLockedByTenantIdAndInvocationId("tenant-1", "invocation-1"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.reserve(
                definition(), request(), "tenant-1", "user-1", "invocation-1", "fingerprint-changed"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("调用幂等标识已用于不同请求");
        verify(sessions, never()).findLockedByIdAndTenantId(any(), any());
    }

    @Test
    void finalizationShouldInsertReservedAssistantAndCasBeforeReleasingSession() {
        AgentSessionRecord session = session();
        session.setActiveInvocationId("invocation-1");
        session.setMessageCount(2);
        AgentCallRecord call = call(AgentInvocationState.PROVIDER_SUCCEEDED);
        call.setProviderResponseText("answer");
        call.setProviderConversationId("conversation-1");
        when(calls.findLockedByTenantIdAndInvocationId("tenant-1", "invocation-1"))
                .thenReturn(Optional.of(call));
        when(sessions.findLockedByIdAndTenantId("session-1", "tenant-1"))
                .thenReturn(Optional.of(session));
        when(messages.saveAndFlush(any())).thenAnswer(invocation -> {
            AgentMessageRecord message = invocation.getArgument(0);
            message.setId("assistant-message-1");
            return message;
        });
        when(calls.compareAndSetSucceeded(
                "tenant-1", "invocation-1", AgentInvocationState.PROVIDER_SUCCEEDED,
                AgentInvocationState.SUCCEEDED, "assistant-message-1"))
                .thenReturn(1);

        var result = service.finalizeSuccess("tenant-1", "invocation-1", "user-1");

        assertThat(result.invocationId()).isEqualTo("invocation-1");
        assertThat(result.text()).isEqualTo("answer");
        assertThat(session.getActiveInvocationId()).isNull();
        assertThat(session.getProviderConversationId()).isEqualTo("conversation-1");
        verify(calls).compareAndSetSucceeded(
                "tenant-1", "invocation-1", AgentInvocationState.PROVIDER_SUCCEEDED,
                AgentInvocationState.SUCCEEDED, "assistant-message-1");
    }

    @Test
    void infrastructureFailureShouldPersistOnlyStablePublicSummary() {
        AgentSessionRecord session = session();
        session.setActiveInvocationId("invocation-1");
        AgentCallRecord call = call(AgentInvocationState.RUNNING);
        call.setExecutionToken("token-1");
        when(calls.findLockedByTenantIdAndInvocationId("tenant-1", "invocation-1"))
                .thenReturn(Optional.of(call));
        when(sessions.findLockedByIdAndTenantId("session-1", "tenant-1"))
                .thenReturn(Optional.of(session));
        when(calls.compareAndSetFailed(
                eq("tenant-1"), eq("invocation-1"), eq("token-1"),
                eq(AgentInvocationState.RUNNING), eq(AgentInvocationState.FAILED),
                any(), eq("IllegalStateException"), eq("智能体上游调用失败")))
                .thenReturn(1);
        AgentInvocationTransactionService.Reservation reservation =
                AgentInvocationTransactionService.Reservation.execute(
                        "invocation-1", "token-1", session, List.of());

        service.fail(reservation,
                new IllegalStateException("endpoint=https://private.example, token=secret"), 12L);

        verify(calls).compareAndSetFailed(
                "tenant-1", "invocation-1", "token-1",
                AgentInvocationState.RUNNING, AgentInvocationState.FAILED,
                12L, "IllegalStateException", "智能体上游调用失败");
    }

    private AgentSessionRecord session() {
        AgentSessionRecord session = new AgentSessionRecord();
        session.setId("session-1");
        session.setTenantId("tenant-1");
        session.setAgentId("agent-1");
        session.setAgentCode("assistant");
        session.setUserId("user-1");
        session.setMessageCount(0);
        session.setSessionState(AgentSessionState.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        return session;
    }

    private AgentCallRecord call(AgentInvocationState state) {
        AgentCallRecord call = new AgentCallRecord();
        call.setTenantId("tenant-1");
        call.setInvocationCode("invocation-1");
        call.setSessionId("session-1");
        call.setReservedUserSequenceNo(1);
        call.setReservedAssistantSequenceNo(2);
        call.setInvocationState(state);
        call.setInputTokens(3);
        call.setOutputTokens(4);
        return call;
    }

    private AgentConfig definition() {
        AgentConfig definition = new AgentConfig();
        definition.setId("agent-1");
        definition.setAgentCode("assistant");
        definition.setProviderType(AgentProviderType.DIFY);
        definition.setInvocationMode(AgentInvocationMode.CHAT);
        return definition;
    }

    private AgentInvocationRequest request() {
        return new AgentInvocationRequest(
                "invocation-1", "session-1", "hello", null, null, null, Map.of());
    }
}
