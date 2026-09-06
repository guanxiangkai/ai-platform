package com.ai.sse.service.impl;

import com.ai.sse.config.SseProperties;
import com.ai.sse.constants.SseConstants;
import com.ai.sse.domain.entity.SseConnectionRecord;
import com.ai.sse.repository.SseConnectionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** PostgreSQL 事务级咨询锁的连接生命周期反序并发回归测试。 */
@Testcontainers(disabledWithoutDocker = false)
class SseConnectionRecordPostgresConcurrencyTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4");

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactions;
    private LifecycleRecordAdapter records;
    private SseConnectionRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        records = new LifecycleRecordAdapter(jdbcTemplate);
        service = new SseConnectionRecordServiceImpl(repository(records), properties(), jdbcTemplate);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sse_connection_record (
                    tenant_id varchar(64) NOT NULL,
                    connection_id varchar(64) NOT NULL,
                    user_id varchar(64) NOT NULL,
                    connect_time timestamp NOT NULL,
                    disconnect_time timestamp,
                    duration_seconds bigint,
                    disconnect_reason varchar(50),
                    connection_status varchar(20),
                    PRIMARY KEY (tenant_id, connection_id)
                )
                """);
        jdbcTemplate.execute("TRUNCATE sse_connection_record");
    }

    @Test
    void disconnectThenDelayedConnectMustSerializeAndKeepFirstTerminalState() throws Exception {
        CountDownLatch disconnectLocked = new CountDownLatch(1);
        CountDownLatch allowDisconnectCommit = new CountDownLatch(1);
        CountDownLatch connectStarted = new CountDownLatch(1);
        CountDownLatch connectFinished = new CountDownLatch(1);
        records.pauseNextLookup(disconnectLocked, allowDisconnectCommit);
        LocalDateTime connectTime = LocalDateTime.of(2026, 9, 7, 10, 0);
        LocalDateTime disconnectTime = connectTime.plusSeconds(45);

        var executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            var disconnect = executor.submit(() -> transactions.executeWithoutResult(ignored ->
                    service.logDisconnect("connection-1", "user-1", "tenant-1",
                            connectTime, disconnectTime, SseConstants.ConnectionStatus.DISCONNECTED)));
            assertThat(disconnectLocked.await(10, TimeUnit.SECONDS)).isTrue();
            var connect = executor.submit(() -> {
                connectStarted.countDown();
                transactions.executeWithoutResult(ignored -> service.logConnect(
                        "connection-1", "user-1", "tenant-1", connectTime, null));
                connectFinished.countDown();
            });
            assertThat(connectStarted.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(connectFinished.await(200, TimeUnit.MILLISECONDS)).isFalse();

            allowDisconnectCommit.countDown();
            disconnect.get(10, TimeUnit.SECONDS);
            connect.get(10, TimeUnit.SECONDS);
        } finally {
            // 任一断言或线程失败时释放正在持锁的事务，避免执行器等待未完成任务。
            allowDisconnectCommit.countDown();
            executor.close();
        }

        SseConnectionRecord record = records.find("tenant-1", "connection-1").orElseThrow();
        assertThat(record.getConnectionStatus()).isEqualTo(SseConstants.ConnectionStatus.DISCONNECTED);
        assertThat(record.getDisconnectReason()).isEqualTo(SseConstants.ConnectionStatus.DISCONNECTED);
        assertThat(record.getDisconnectTime()).isEqualTo(disconnectTime);
        assertThat(record.getDurationSeconds()).isEqualTo(45L);
    }

    private SseConnectionRecordRepository repository(LifecycleRecordAdapter adapter) {
        SseConnectionRecordRepository repository = mock(SseConnectionRecordRepository.class);
        when(repository.findByTenantIdAndConnectionIdAndDeletedFalse(anyString(), anyString()))
                .thenAnswer(invocation -> adapter.find(invocation.getArgument(0), invocation.getArgument(1)));
        doAnswer(invocation -> adapter.save(invocation.getArgument(0)))
                .when(repository).save(any(SseConnectionRecord.class));
        return repository;
    }

    private SseProperties properties() {
        return new SseProperties(null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    /** 将 repository 调用委托给同一 JDBC 数据源的最小测试表，不覆盖真实 ORM 映射。 */
    private static final class LifecycleRecordAdapter {
        private final JdbcTemplate jdbcTemplate;
        private CountDownLatch lookupEntered;
        private CountDownLatch allowLookup;

        private LifecycleRecordAdapter(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        private void pauseNextLookup(CountDownLatch entered, CountDownLatch allow) {
            lookupEntered = entered;
            allowLookup = allow;
        }

        private Optional<SseConnectionRecord> find(String tenantId, String connectionId) {
            awaitPausedLookup();
            return jdbcTemplate.query("""
                            SELECT tenant_id, connection_id, user_id, connect_time, disconnect_time,
                                   duration_seconds, disconnect_reason, connection_status
                            FROM sse_connection_record
                            WHERE tenant_id = ? AND connection_id = ?
                            """, result -> result.next() ? Optional.of(record(result)) : Optional.empty(),
                    tenantId, connectionId);
        }

        private SseConnectionRecord save(SseConnectionRecord record) {
            jdbcTemplate.update("""
                            INSERT INTO sse_connection_record (
                                tenant_id, connection_id, user_id, connect_time, disconnect_time,
                                duration_seconds, disconnect_reason, connection_status
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT (tenant_id, connection_id) DO UPDATE SET
                                user_id = EXCLUDED.user_id,
                                connect_time = EXCLUDED.connect_time,
                                disconnect_time = EXCLUDED.disconnect_time,
                                duration_seconds = EXCLUDED.duration_seconds,
                                disconnect_reason = EXCLUDED.disconnect_reason,
                                connection_status = EXCLUDED.connection_status
                            """,
                    record.getTenantId(), record.getConnectionId(), record.getUserId(), record.getConnectTime(),
                    record.getDisconnectTime(), record.getDurationSeconds(), record.getDisconnectReason(),
                    record.getConnectionStatus());
            return record;
        }

        private void awaitPausedLookup() {
            CountDownLatch entered = lookupEntered;
            CountDownLatch allow = allowLookup;
            if (entered == null || allow == null) return;
            lookupEntered = null;
            allowLookup = null;
            entered.countDown();
            try {
                if (!allow.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("测试未释放连接生命周期事务");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("连接生命周期测试被中断", exception);
            }
        }

        private SseConnectionRecord record(ResultSet result) throws java.sql.SQLException {
            SseConnectionRecord record = new SseConnectionRecord();
            record.setTenantId(result.getString("tenant_id"));
            record.setConnectionId(result.getString("connection_id"));
            record.setUserId(result.getString("user_id"));
            record.setConnectTime(result.getObject("connect_time", LocalDateTime.class));
            record.setDisconnectTime(result.getObject("disconnect_time", LocalDateTime.class));
            long duration = result.getLong("duration_seconds");
            record.setDurationSeconds(result.wasNull() ? null : duration);
            record.setDisconnectReason(result.getString("disconnect_reason"));
            record.setConnectionStatus(result.getString("connection_status"));
            record.setServerInstance("test-server");
            return record;
        }
    }
}
