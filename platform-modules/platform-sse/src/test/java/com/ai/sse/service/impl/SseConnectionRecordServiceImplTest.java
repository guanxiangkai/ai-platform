package com.ai.sse.service.impl;

import com.ai.sse.config.SseProperties;
import com.ai.sse.constants.SseConstants;
import com.ai.sse.domain.entity.SseConnectionRecord;
import com.ai.sse.repository.SseConnectionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SSE 连接审计生命周期状态收敛测试。 */
class SseConnectionRecordServiceImplTest {
    private SseConnectionRecordRepository repository;
    private JdbcTemplate jdbcTemplate;
    private SseConnectionRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(SseConnectionRecordRepository.class);
        jdbcTemplate = lockingJdbcTemplate();
        service = new SseConnectionRecordServiceImpl(repository, new SseProperties(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null), jdbcTemplate);
    }

    @Test
    void disconnectBeforeConnectShouldCreateTerminalRecordWithOriginalMetadata() {
        LocalDateTime connectTime = LocalDateTime.of(2026, 9, 7, 10, 0);
        LocalDateTime disconnectTime = connectTime.plusSeconds(45);
        when(repository.findByTenantIdAndConnectionIdAndDeletedFalse("tenant-1", "connection-1"))
                .thenReturn(Optional.empty());

        service.logDisconnect("connection-1", "user-1", "tenant-1",
                connectTime, disconnectTime, SseConstants.ConnectionStatus.DISCONNECTED);

        var record = org.mockito.ArgumentCaptor.forClass(SseConnectionRecord.class);
        verify(repository).save(record.capture());
        assertThat(record.getValue())
                .extracting(SseConnectionRecord::getTenantId, SseConnectionRecord::getConnectionId,
                        SseConnectionRecord::getUserId, SseConnectionRecord::getConnectTime,
                        SseConnectionRecord::getDisconnectTime, SseConnectionRecord::getConnectionStatus,
                        SseConnectionRecord::getDisconnectReason, SseConnectionRecord::getDurationSeconds)
                .containsExactly("tenant-1", "connection-1", "user-1", connectTime, disconnectTime,
                        SseConstants.ConnectionStatus.DISCONNECTED, SseConstants.ConnectionStatus.DISCONNECTED, 45L);
    }

    @Test
    void delayedConnectMustNotResetExistingTerminalRecord() {
        LocalDateTime connectTime = LocalDateTime.of(2026, 9, 7, 10, 0);
        LocalDateTime disconnectTime = connectTime.plusSeconds(45);
        SseConnectionRecord record = terminalRecord(connectTime, disconnectTime,
                SseConstants.ConnectionStatus.TIMEOUT);
        when(repository.findByTenantIdAndConnectionIdAndDeletedFalse("tenant-1", "connection-1"))
                .thenReturn(Optional.of(record));

        service.logConnect("connection-1", "user-1", "tenant-1", connectTime.plusMinutes(1), "127.0.0.1");

        assertThat(record.getConnectionStatus()).isEqualTo(SseConstants.ConnectionStatus.TIMEOUT);
        assertThat(record.getDisconnectTime()).isEqualTo(disconnectTime);
        assertThat(record.getDisconnectReason()).isEqualTo(SseConstants.ConnectionStatus.TIMEOUT);
        verify(repository, never()).save(any());
    }

    @Test
    void connectThenDisconnectShouldPersistTerminalStateAndDuration() {
        LocalDateTime connectTime = LocalDateTime.of(2026, 9, 7, 10, 0);
        SseConnectionRecord record = new SseConnectionRecord();
        record.setTenantId("tenant-1");
        record.setConnectionId("connection-1");
        record.setUserId("user-1");
        record.setConnectTime(connectTime);
        record.setConnectionStatus(SseConstants.ConnectionStatus.CONNECTED);
        when(repository.findByTenantIdAndConnectionIdAndDeletedFalse("tenant-1", "connection-1"))
                .thenReturn(Optional.of(record));

        service.logDisconnect("connection-1", "user-1", "tenant-1", connectTime,
                connectTime.plusSeconds(45), SseConstants.ConnectionStatus.DISCONNECTED);

        assertThat(record.getConnectionStatus()).isEqualTo(SseConstants.ConnectionStatus.DISCONNECTED);
        assertThat(record.getDisconnectReason()).isEqualTo(SseConstants.ConnectionStatus.DISCONNECTED);
        assertThat(record.getDurationSeconds()).isEqualTo(45L);
        verify(repository).save(record);
    }

    @Test
    void repeatedDisconnectMustKeepFirstTerminalFactAndUseTenantInLookup() {
        LocalDateTime connectTime = LocalDateTime.of(2026, 9, 7, 10, 0);
        LocalDateTime firstDisconnectTime = connectTime.plusSeconds(45);
        SseConnectionRecord record = terminalRecord(connectTime, firstDisconnectTime,
                SseConstants.ConnectionStatus.DISCONNECTED);
        when(repository.findByTenantIdAndConnectionIdAndDeletedFalse("tenant-1", "connection-1"))
                .thenReturn(Optional.of(record));

        service.logDisconnect("connection-1", "user-1", "tenant-1", connectTime,
                connectTime.plusMinutes(2), SseConstants.ConnectionStatus.ERROR);

        assertThat(record.getDisconnectTime()).isEqualTo(firstDisconnectTime);
        assertThat(record.getDisconnectReason()).isEqualTo(SseConstants.ConnectionStatus.DISCONNECTED);
        verify(repository).findByTenantIdAndConnectionIdAndDeletedFalse("tenant-1", "connection-1");
        verify(repository, never()).save(any());
    }

    @Test
    void sameConnectionIdInAnotherTenantMustCreateIndependentTerminalRecord() {
        LocalDateTime connectTime = LocalDateTime.of(2026, 9, 7, 10, 0);
        when(repository.findByTenantIdAndConnectionIdAndDeletedFalse("tenant-2", "connection-1"))
                .thenReturn(Optional.empty());

        service.logDisconnect("connection-1", "user-2", "tenant-2", connectTime,
                connectTime.plusSeconds(1), SseConstants.ConnectionStatus.DISCONNECTED);

        var record = org.mockito.ArgumentCaptor.forClass(SseConnectionRecord.class);
        verify(repository).save(record.capture());
        assertThat(record.getValue().getTenantId()).isEqualTo("tenant-2");
    }

    @Test
    void lifecyclePersistenceFailureMustRemainObservableToAsyncInfrastructure() {
        when(jdbcTemplate.execute(any(ConnectionCallback.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> service.logConnect(
                "connection-1", "user-1", "tenant-1", LocalDateTime.now(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }

    private JdbcTemplate lockingJdbcTemplate() {
        JdbcTemplate template = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        try {
            when(connection.prepareStatement(anyString())).thenReturn(statement);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        doAnswer(invocation -> {
            ConnectionCallback<?> callback = invocation.getArgument(0);
            return callback.doInConnection(connection);
        }).when(template).execute(any(ConnectionCallback.class));
        return template;
    }

    private SseConnectionRecord terminalRecord(
            LocalDateTime connectTime, LocalDateTime disconnectTime, String reason) {
        SseConnectionRecord record = new SseConnectionRecord();
        record.setTenantId("tenant-1");
        record.setConnectionId("connection-1");
        record.setUserId("user-1");
        record.setConnectTime(connectTime);
        record.setDisconnectTime(disconnectTime);
        record.setDurationSeconds(45L);
        record.setConnectionStatus(reason);
        record.setDisconnectReason(reason);
        record.setClientIp("127.0.0.1");
        record.setServerInstance("server-1");
        return record;
    }
}
