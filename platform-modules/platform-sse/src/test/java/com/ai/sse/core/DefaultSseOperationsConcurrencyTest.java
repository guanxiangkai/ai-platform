package com.ai.sse.core;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefaultSseOperationsConcurrencyTest {

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
