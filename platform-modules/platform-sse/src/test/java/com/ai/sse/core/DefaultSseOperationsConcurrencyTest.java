package com.ai.sse.core;

import com.ai.sse.model.SseMessage;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSseOperationsConcurrencyTest {

    @Test
    void broadcastToTenant_sendsOnlyToConnectionsInTargetTenantForTheSameUser() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any())).thenReturn("serialized-message");
        DefaultSseOperations operations = new DefaultSseOperations(60_000, 4, 2,
                objectMapper, mock(ApplicationEventPublisher.class));
        List<String> tenantAEvents = new ArrayList<>();
        List<String> tenantBEvents = new ArrayList<>();

        var tenantASubscription = operations.connect("same-user", "tenant-a")
                .subscribe(event -> tenantAEvents.add(event.data()));
        var tenantBSubscription = operations.connect("same-user", "tenant-b")
                .subscribe(event -> tenantBEvents.add(event.data()));
        tenantAEvents.clear();
        tenantBEvents.clear();

        operations.broadcastToTenant("tenant-a", SseMessage.notification("tenant message"));

        assertThat(tenantAEvents).containsExactly("serialized-message");
        assertThat(tenantBEvents).isEmpty();
        verify(objectMapper, times(3)).writeValueAsString(org.mockito.ArgumentMatchers.any());
        tenantASubscription.dispose();
        tenantBSubscription.dispose();
    }

    @Test
    void broadcastToTenant_doesNotSendWhenNoMatchingConnectionExists() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any())).thenReturn("serialized-message");
        DefaultSseOperations operations = new DefaultSseOperations(60_000, 4, 1,
                objectMapper, mock(ApplicationEventPublisher.class));
        List<String> tenantBEvents = new ArrayList<>();

        var tenantBSubscription = operations.connect("user", "tenant-b")
                .subscribe(event -> tenantBEvents.add(event.data()));
        tenantBEvents.clear();

        operations.broadcastToTenant("tenant-a", SseMessage.notification("tenant message"));

        assertThat(tenantBEvents).isEmpty();
        verify(objectMapper).writeValueAsString(org.mockito.ArgumentMatchers.any());
        tenantBSubscription.dispose();
    }

    @Test
    void broadcastToTenant_doesNotSendToAClosedMatchingConnection() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any())).thenReturn("serialized-message");
        DefaultSseOperations operations = new DefaultSseOperations(60_000, 4, 1,
                objectMapper, mock(ApplicationEventPublisher.class));

        var tenantASubscription = operations.connect("user", "tenant-a").subscribe();
        tenantASubscription.dispose();

        operations.broadcastToTenant("tenant-a", SseMessage.notification("tenant message"));

        assertThat(operations.isOnline("user")).isFalse();
        verify(objectMapper).writeValueAsString(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void connect_neverExceedsGlobalCapacityUnderConcurrentRequests() throws Exception {
        int limit = 4;
        DefaultSseOperations operations = new DefaultSseOperations(60_000, limit, 1,
                mock(ObjectMapper.class), mock(ApplicationEventPublisher.class));
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch ready = new CountDownLatch(16);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<? extends java.util.concurrent.Future<?>> futures = java.util.stream.IntStream.range(0, 16)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        try {
                            start.await();
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(exception);
                        }
                        operations.connect("user-" + index, "tenant-1");
                    }))
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var future : futures) future.get(5, TimeUnit.SECONDS);

            assertThat(operations.getMetrics().get("totalActiveConnections")).isEqualTo(limit);
        } finally {
            executor.shutdownNow();
        }
    }
}
