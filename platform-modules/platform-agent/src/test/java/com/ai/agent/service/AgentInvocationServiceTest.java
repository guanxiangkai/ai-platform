package com.ai.agent.service;

import com.ai.agent.domain.AgentInvocationMode;
import com.ai.agent.domain.AgentProviderType;
import com.ai.agent.domain.AgentPublishState;
import com.ai.agent.domain.dto.AgentInvocationRequest;
import com.ai.agent.domain.entity.AgentConfig;
import com.ai.agent.domain.entity.AgentSessionRecord;
import com.ai.agent.domain.vo.AgentViews;
import com.ai.agent.integration.AgentProviderClient;
import com.ai.agent.integration.AgentProviderRegistry;
import com.ai.agent.integration.AgentProviderResult;
import com.ai.agent.repository.AgentConfigRepository;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentInvocationServiceTest {
    private AgentManagementService management;
    private AgentProviderRegistry providers;
    private AgentInvocationTransactionService transactions;
    private AgentInvocationService service;

    @BeforeEach
    void setUp() {
        management = mock(AgentManagementService.class);
        providers = mock(AgentProviderRegistry.class);
        transactions = mock(AgentInvocationTransactionService.class);
        service = new AgentInvocationService(
                management, mock(AgentConfigRepository.class), providers, transactions);
        UserContextHolder.set(new UserContext(
                "user-1", "tenant-1", false, null, Set.of(), Set.of(), Set.of(), Map.of()));
    }

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void completedIdempotentInvocationShouldReturnWithoutCallingProvider() {
        AgentConfig definition = definition();
        AgentInvocationRequest request = request("invocation-1");
        AgentViews.InvocationResult completed = new AgentViews.InvocationResult(
                "session-1", "invocation-1", "message-2", "answer", 3, 4);
        when(management.requireDefinition("agent-1")).thenReturn(definition);
        when(transactions.reserve(any(), any(), any(), any(), any(), any()))
                .thenReturn(AgentInvocationTransactionService.Reservation.completed(completed));

        AgentViews.InvocationResult result = service.invoke("agent-1", request);

        assertThat(result).isSameAs(completed);
        verify(providers, never()).require(any());
    }

    @Test
    void providerCallShouldRunBetweenReservationAndRecoverableCompletionTransactions() {
        AgentConfig definition = definition();
        AgentInvocationRequest request = request("invocation-1");
        AgentSessionRecord session = new AgentSessionRecord();
        session.setId("session-1");
        session.setTenantId("tenant-1");
        AgentInvocationTransactionService.Reservation reservation =
                AgentInvocationTransactionService.Reservation.execute(
                        "invocation-1", "token-1", session, List.of());
        AgentProviderClient provider = mock(AgentProviderClient.class);
        AgentProviderResult providerResult = new AgentProviderResult("answer", 3, 4, "conversation-1");
        AgentViews.InvocationResult completed = new AgentViews.InvocationResult(
                "session-1", "invocation-1", "message-2", "answer", 3, 4);
        when(management.requireDefinition("agent-1")).thenReturn(definition);
        when(transactions.reserve(any(), any(), any(), any(), any(), any())).thenReturn(reservation);
        when(providers.require(AgentProviderType.DIFY)).thenReturn(provider);
        when(provider.invoke(any(), any())).thenReturn(providerResult);
        when(transactions.finalizeSuccess("tenant-1", "invocation-1", "user-1"))
                .thenReturn(completed);

        AgentViews.InvocationResult result = service.invoke("agent-1", request);

        assertThat(result).isSameAs(completed);
        var ordered = inOrder(transactions, provider);
        ordered.verify(transactions).reserve(any(), any(), any(), any(), any(), any());
        ordered.verify(provider).invoke(any(), any());
        ordered.verify(transactions).recordProviderSuccess(any(), any(), anyLong());
        ordered.verify(transactions).finalizeSuccess("tenant-1", "invocation-1", "user-1");
    }

    @Test
    void providerInfrastructureDetailsShouldNotReachApiError() {
        AgentConfig definition = definition();
        AgentInvocationRequest request = request("invocation-1");
        AgentSessionRecord session = new AgentSessionRecord();
        session.setId("session-1");
        session.setTenantId("tenant-1");
        AgentInvocationTransactionService.Reservation reservation =
                AgentInvocationTransactionService.Reservation.execute(
                        "invocation-1", "token-1", session, List.of());
        AgentProviderClient provider = mock(AgentProviderClient.class);
        when(management.requireDefinition("agent-1")).thenReturn(definition);
        when(transactions.reserve(any(), any(), any(), any(), any(), any())).thenReturn(reservation);
        when(providers.require(AgentProviderType.DIFY)).thenReturn(provider);
        when(provider.invoke(any(), any())).thenThrow(
                new IllegalStateException("endpoint=https://private.example, token=secret"));

        assertThatThrownBy(() -> service.invoke("agent-1", request))
                .hasMessage("智能体上游调用失败")
                .hasMessageNotContaining("private.example")
                .hasMessageNotContaining("secret");
        verify(transactions).fail(any(), any(IllegalStateException.class), anyLong());
    }

    private AgentConfig definition() {
        AgentConfig definition = new AgentConfig();
        definition.setId("agent-1");
        definition.setAgentCode("assistant");
        definition.setProviderType(AgentProviderType.DIFY);
        definition.setInvocationMode(AgentInvocationMode.CHAT);
        definition.setPublishState(AgentPublishState.PUBLISHED);
        definition.setEnabled(true);
        definition.setCredential("encrypted-credential");
        return definition;
    }

    private AgentInvocationRequest request(String invocationId) {
        return new AgentInvocationRequest(
                invocationId, null, "hello", null, null, null, Map.of());
    }
}
