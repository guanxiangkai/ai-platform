package com.ai.sse.config;

import com.ai.sse.core.SseOperations;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SseMaintenanceContractTest {

    private final SseAutoConfiguration configuration = new SseAutoConfiguration();
    private final SseOperations operations = mock(SseOperations.class);

    @Test
    void heartbeatInvokesReplacementImplementationWithoutDependingOnItsConcreteType() {
        when(operations.getOnlineCount()).thenReturn(2);
        when(operations.sendHeartbeatToAll()).thenReturn(3);

        configuration.sseHeartbeatScheduler(operations).sendHeartbeat();

        verify(operations).sendHeartbeatToAll();
    }

    @Test
    void heartbeatSkipsSendingWhenThereAreNoOnlineUsers() {
        when(operations.getOnlineCount()).thenReturn(0);

        configuration.sseHeartbeatScheduler(operations).sendHeartbeat();

        verify(operations, never()).sendHeartbeatToAll();
    }

    @Test
    void cleanupInvokesReplacementImplementationEvenWhenThereAreNoOnlineUsers() {
        when(operations.cleanupExpiredConnections()).thenReturn(1);

        configuration.sseConnectionCleanupTask(operations).cleanup();

        verify(operations).cleanupExpiredConnections();
        verify(operations, never()).getOnlineCount();
    }
}
