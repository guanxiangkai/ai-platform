package com.ai.sse.listener;

import com.ai.sse.constants.SseConstants;
import com.ai.sse.event.SseDisconnectEvent;
import com.ai.sse.model.SseConnection;
import com.ai.sse.service.ISseConnectionRecordService;
import com.ai.sse.service.ISseLogService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SseConnectionEventListenerTest {

    @Test
    void disconnectShouldExplicitlyPassConnectionUserAndTenantToAuditLog() {
        ISseConnectionRecordService connectionRecordService = mock(ISseConnectionRecordService.class);
        ISseLogService sseLogService = mock(ISseLogService.class);
        SseConnectionEventListener listener = new SseConnectionEventListener(
                connectionRecordService, sseLogService);
        SseConnection connection = new SseConnection(
                "connection-1", "user-1", null, "tenant-1", SseConstants.ConnectionStatus.DISCONNECTED,
                LocalDateTime.of(2026, 8, 30, 10, 0), LocalDateTime.of(2026, 8, 30, 10, 5));

        listener.onDisconnect(new SseDisconnectEvent(this, connection));

        verify(connectionRecordService).logDisconnect(
                eq("connection-1"), eq("user-1"), eq("tenant-1"),
                eq(connection.connectTime()), any(LocalDateTime.class),
                eq(SseConstants.ConnectionStatus.DISCONNECTED));
        verify(sseLogService).logDisconnect(
                eq("connection-1"), eq("user-1"), eq("tenant-1"), any(LocalDateTime.class),
                eq(SseConstants.ConnectionStatus.DISCONNECTED));
    }
}
