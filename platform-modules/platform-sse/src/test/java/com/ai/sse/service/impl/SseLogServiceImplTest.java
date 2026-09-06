package com.ai.sse.service.impl;

import com.ai.sse.domain.SseOperationType;
import com.ai.sse.domain.entity.SseLog;
import com.ai.sse.repository.SseLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SseLogServiceImplTest {

    @Test
    void disconnectShouldPersistExplicitUserAndTenantFromConnection() {
        SseLogRepository repository = mock(SseLogRepository.class);
        SseLogServiceImpl service = new SseLogServiceImpl(repository);
        LocalDateTime disconnectTime = LocalDateTime.of(2026, 8, 30, 10, 30);

        service.logDisconnect("connection-1", "user-1", "tenant-1", disconnectTime, "DISCONNECTED");

        ArgumentCaptor<SseLog> entry = ArgumentCaptor.forClass(SseLog.class);
        verify(repository).save(entry.capture());
        assertThat(entry.getValue())
                .extracting(SseLog::getOperationType, SseLog::getConnectionId, SseLog::getUserId,
                        SseLog::getTenantId, SseLog::getLogTime, SseLog::getFailReason)
                .containsExactly(SseOperationType.DISCONNECT, "connection-1", "user-1", "tenant-1",
                        disconnectTime, "DISCONNECTED");
    }
}
