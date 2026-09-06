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
import com.ai.agent.integration.AgentProviderStreamEvent;
import com.ai.agent.repository.AgentConfigRepository;
import io.github.guanxiangkai.jpa.plus.core.field.FieldEngine;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentInvocationServiceTest {
    private AgentManagementService management;
    private AgentProviderRegistry providers;
    private AgentInvocationTransactionService transactions;
    private FieldEngine fieldEngine;
    private AgentInvocationService service;

    @BeforeEach
    void setUp() {
        management = mock(AgentManagementService.class);
        providers = mock(AgentProviderRegistry.class);
        transactions = mock(AgentInvocationTransactionService.class);
        fieldEngine = mock(FieldEngine.class);
        service = new AgentInvocationService(
                management, mock(AgentConfigRepository.class), fieldEngine, providers, transactions);
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
        verify(fieldEngine).afterQuery(definition, AgentConfig.class);
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
    void streamShouldPersistCompletionAndForwardIncrementalEvents() {
        AgentConfig definition = definition();
        AgentInvocationRequest request = request("invocation-1");
        AgentInvocationTransactionService.Reservation reservation = reservation();
        AgentProviderClient provider = mock(AgentProviderClient.class);
        AgentProviderResult providerResult = new AgentProviderResult("answer", 3, 4, "conversation-1");
        AgentViews.InvocationResult completed = new AgentViews.InvocationResult(
                "session-1", "invocation-1", "message-2", "answer", 3, 4);
        when(management.requireDefinition("agent-1")).thenReturn(definition);
        when(transactions.reserve(any(), any(), any(), any(), any(), any())).thenReturn(reservation);
        when(providers.require(AgentProviderType.DIFY)).thenReturn(provider);
        when(provider.stream(any(), any())).thenReturn(Flux.just(
                AgentProviderStreamEvent.delta("ans"), AgentProviderStreamEvent.complete(providerResult)));
        when(transactions.finalizeSuccess("tenant-1", "invocation-1", "user-1")).thenReturn(completed);

        assertThat(service.stream("agent-1", request).collectList().block())
                .extracting(AgentInvocationService.StreamEvent::type)
                .containsExactly(
                        AgentInvocationService.StreamEvent.Type.START,
                        AgentInvocationService.StreamEvent.Type.DELTA,
                        AgentInvocationService.StreamEvent.Type.COMPLETE);

        verify(transactions).recordProviderSuccess(any(), eq(providerResult), anyLong());
        verify(transactions).finalizeSuccess("tenant-1", "invocation-1", "user-1");
        verify(transactions, never()).fail(any(), any(), anyLong());
    }

    @Test
    void completedIdempotentStreamShouldReplayPersistedResultWithoutCallingProvider() {
        AgentConfig definition = definition();
        AgentInvocationRequest request = request("invocation-1");
        AgentViews.InvocationResult completed = new AgentViews.InvocationResult(
                "session-1", "invocation-1", "message-2", "answer", 3, 4);
        when(management.requireDefinition("agent-1")).thenReturn(definition);
        when(transactions.reserve(any(), any(), any(), any(), any(), any()))
                .thenReturn(AgentInvocationTransactionService.Reservation.completed(completed));

        assertThat(service.stream("agent-1", request).collectList().block())
                .extracting(AgentInvocationService.StreamEvent::type)
                .containsExactly(
                        AgentInvocationService.StreamEvent.Type.START,
                        AgentInvocationService.StreamEvent.Type.COMPLETE);

        verify(transactions).reserve(
                eq(definition), eq(request), eq("tenant-1"), eq("user-1"), eq("invocation-1"), any());
        verify(providers, never()).require(any());
    }

    @Test
    void streamShouldFinalizePersistedProviderResultWithoutCallingProviderAgain() {
        AgentConfig definition = definition();
        AgentInvocationRequest request = request("invocation-1");
        AgentViews.InvocationResult completed = new AgentViews.InvocationResult(
                "session-1", "invocation-1", "message-2", "answer", 3, 4);
        when(management.requireDefinition("agent-1")).thenReturn(definition);
        when(transactions.reserve(any(), any(), any(), any(), any(), any()))
                .thenReturn(AgentInvocationTransactionService.Reservation.finalizePending(
                        "tenant-1", "invocation-1"));
        when(transactions.finalizeSuccess("tenant-1", "invocation-1", "user-1")).thenReturn(completed);

        assertThat(service.stream("agent-1", request).collectList().block())
                .extracting(AgentInvocationService.StreamEvent::type)
                .containsExactly(
                        AgentInvocationService.StreamEvent.Type.START,
                        AgentInvocationService.StreamEvent.Type.COMPLETE);

        verify(providers, never()).require(any());
    }

    @Test
    void streamShouldPersistProviderFailureAndReturnErrorEvent() {
        AgentConfig definition = definition();
        AgentInvocationRequest request = request("invocation-1");
        AgentProviderClient provider = mock(AgentProviderClient.class);
        when(management.requireDefinition("agent-1")).thenReturn(definition);
        when(transactions.reserve(any(), any(), any(), any(), any(), any())).thenReturn(reservation());
        when(providers.require(AgentProviderType.DIFY)).thenReturn(provider);
        when(provider.stream(any(), any())).thenReturn(Flux.error(new IllegalStateException("upstream unavailable")));

        assertThat(service.stream("agent-1", request).collectList().block())
                .extracting(AgentInvocationService.StreamEvent::type)
                .containsExactly(
                        AgentInvocationService.StreamEvent.Type.START,
                        AgentInvocationService.StreamEvent.Type.ERROR);

        verify(transactions).fail(any(), any(), anyLong());
    }

    @Test
    void streamCancellationShouldReleaseReservation() {
        AgentConfig definition = definition();
        AgentInvocationRequest request = request("invocation-1");
        AgentProviderClient provider = mock(AgentProviderClient.class);
        AtomicBoolean providerSubscribed = new AtomicBoolean();
        when(management.requireDefinition("agent-1")).thenReturn(definition);
        when(transactions.reserve(any(), any(), any(), any(), any(), any())).thenReturn(reservation());
        when(providers.require(AgentProviderType.DIFY)).thenReturn(provider);
        when(provider.stream(any(), any())).thenReturn(Flux.<AgentProviderStreamEvent>never()
                .doOnSubscribe(ignored -> providerSubscribed.set(true)));

        assertThat(service.stream("agent-1", request).take(1).collectList().block())
                .extracting(AgentInvocationService.StreamEvent::type)
                .containsExactly(AgentInvocationService.StreamEvent.Type.START);

        verify(transactions, timeout(1_000)).fail(any(), any(), anyLong());
        assertThat(providerSubscribed).isFalse();
    }

    @Test
    void streamShouldReleaseReservationWhenProviderLookupThrows() {
        var reservation = reservation();
        when(management.requireDefinition("agent-1")).thenReturn(definition());
        when(transactions.reserve(any(), any(), any(), any(), any(), any())).thenReturn(reservation);
        when(providers.require(AgentProviderType.DIFY)).thenThrow(new IllegalStateException(
                "https://provider.example.invalid/?token=fake-private-detail"));

        var events = service.stream("agent-1", request("invocation-1")).collectList().block();

        assertThat(events).extracting(AgentInvocationService.StreamEvent::type)
                .containsExactly(AgentInvocationService.StreamEvent.Type.START,
                        AgentInvocationService.StreamEvent.Type.ERROR);
        assertThat(events.getLast().message()).isEqualTo("智能体服务暂不可用，请稍后重试");
        verify(transactions).fail(eq(reservation), any(), anyLong());
    }

    @Test
    void streamShouldReleaseReservationWhenProviderStreamFactoryThrows() {
        var reservation = reservation();
        AgentProviderClient provider = mock(AgentProviderClient.class);
        when(management.requireDefinition("agent-1")).thenReturn(definition());
        when(transactions.reserve(any(), any(), any(), any(), any(), any())).thenReturn(reservation);
        when(providers.require(AgentProviderType.DIFY)).thenReturn(provider);
        when(provider.stream(any(), any())).thenThrow(new IllegalStateException("fake-private-detail"));

        var events = service.stream("agent-1", request("invocation-1")).collectList().block();

        assertThat(events.getLast().type()).isEqualTo(AgentInvocationService.StreamEvent.Type.ERROR);
        assertThat(events.getLast().message()).isEqualTo("智能体服务暂不可用，请稍后重试");
        verify(transactions).fail(eq(reservation), any(), anyLong());
    }

    @Test
    void streamShouldNotMarkPersistedProviderResultAsFailedWhenFinalizationMustBeRetried() {
        AgentConfig definition = definition();
        AgentInvocationRequest request = request("invocation-1");
        AgentProviderClient provider = mock(AgentProviderClient.class);
        AgentProviderResult providerResult = new AgentProviderResult("answer", 3, 4, "conversation-1");
        when(management.requireDefinition("agent-1")).thenReturn(definition);
        when(transactions.reserve(any(), any(), any(), any(), any(), any())).thenReturn(reservation());
        when(providers.require(AgentProviderType.DIFY)).thenReturn(provider);
        when(provider.stream(any(), any())).thenReturn(Flux.just(AgentProviderStreamEvent.complete(providerResult)));
        when(transactions.finalizeSuccess("tenant-1", "invocation-1", "user-1"))
                .thenThrow(new IllegalStateException("temporary persistence failure"));

        assertThat(service.stream("agent-1", request).collectList().block())
                .extracting(AgentInvocationService.StreamEvent::type)
                .containsExactly(
                        AgentInvocationService.StreamEvent.Type.START,
                        AgentInvocationService.StreamEvent.Type.ERROR);

        verify(transactions).recordProviderSuccess(any(), eq(providerResult), anyLong());
        verify(transactions, never()).fail(any(), any(), anyLong());
    }

    private AgentInvocationTransactionService.Reservation reservation() {
        AgentSessionRecord session = new AgentSessionRecord();
        session.setId("session-1");
        session.setTenantId("tenant-1");
        return AgentInvocationTransactionService.Reservation.execute(
                "invocation-1", "token-1", session, List.of());
    }

    private AgentConfig definition() {
        AgentConfig definition = new AgentConfig();
        definition.setId("agent-1");
        definition.setAgentCode("assistant");
        definition.setProviderType(AgentProviderType.DIFY);
        definition.setInvocationMode(AgentInvocationMode.CHAT);
        definition.setPublishState(AgentPublishState.PUBLISHED);
        definition.setEnabled(true);
        definition.setCredential("v1:encrypted-credential");
        return definition;
    }

    private AgentInvocationRequest request(String invocationId) {
        return new AgentInvocationRequest(
                invocationId, null, "hello", null, null, null, Map.of());
    }
}
