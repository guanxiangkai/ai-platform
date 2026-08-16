package com.ai.agent.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 使用真实 PostgreSQL 验证会话行锁和幂等约束。 */
@Testcontainers
class AgentInvocationPostgresConcurrencyTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:latest");

    @BeforeAll
    static void createAgentInvocationSchema() throws Exception {
        String baseline = Files.readString(repositoryRoot()
                .resolve("deploy/database/V001__create_platform_schema.sql"));
        try (Connection connection = connection()) {
            execute(connection, """
                    CREATE TABLE public.ai_agent_config (
                        tenant_id varchar(64) NOT NULL,
                        id varchar(64) NOT NULL,
                        CONSTRAINT uk_agent_config_tenant_id UNIQUE (tenant_id, id)
                    )
                    """);
            execute(connection, createTable(baseline, "ai_agent_session_record"));
            execute(connection, createTable(baseline, "ai_agent_message_record"));
            execute(connection, createTable(baseline, "ai_agent_call_record"));
            execute(connection, statement(baseline, "CREATE UNIQUE INDEX uk_ai_agent_message_session_sequence_active"));
            execute(connection, statement(baseline, "CREATE UNIQUE INDEX uk_ai_agent_message_invocation_role_active"));
            execute(connection, statement(baseline, "CREATE UNIQUE INDEX uk_ai_agent_call_tenant_code_active"));
            execute(connection, "INSERT INTO public.ai_agent_config (tenant_id, id) VALUES ('tenant-1', 'agent-1')");
            execute(connection, """
                    INSERT INTO public.ai_agent_session_record (
                        id, deleted, version, enabled, tenant_id, session_code, agent_id, agent_code,
                        user_id, message_count, session_state, started_at
                    ) VALUES (
                        'session-1', false, 0, true, 'tenant-1', 'session-code-1', 'agent-1',
                        'assistant', 'user-1', 0, 'ACTIVE', now()
                    )
                    """);
        }
    }

    @Test
    void concurrentReservationsShouldSerializeAndOnlyOneInvocationShouldOwnSession() throws Exception {
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> reserve(
                    "invocation-1", "message-1", firstLocked, null, allowFirstCommit));
            assertThat(firstLocked.await(10, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> reserve(
                    "invocation-2", "message-2", secondStarted, firstLocked, null));
            assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
            allowFirstCommit.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(10, TimeUnit.SECONDS)).isFalse();
        }

        try (Connection connection = connection();
             var statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT invocation_id, sequence_no
                     FROM public.ai_agent_message_record
                     WHERE tenant_id = 'tenant-1' AND session_id = 'session-1'
                     ORDER BY sequence_no
                     """)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString("invocation_id")).isEqualTo("invocation-1");
            assertThat(rows.getInt("sequence_no")).isEqualTo(1);
            assertThat(rows.next()).isFalse();
        }
    }

    @Test
    void baselineShouldExposeDatabaseIdempotencyAndSequenceConstraints() throws Exception {
        try (Connection connection = connection();
             var statement = connection.prepareStatement("""
                     SELECT indexname
                     FROM pg_indexes
                     WHERE schemaname = 'public'
                       AND indexname IN (
                         'uk_ai_agent_message_session_sequence_active',
                         'uk_ai_agent_message_invocation_role_active',
                         'uk_ai_agent_call_tenant_code_active'
                       )
                     ORDER BY indexname
                     """);
             ResultSet indexes = statement.executeQuery()) {
            assertThat(indexes.next()).isTrue();
            assertThat(indexes.getString(1)).isEqualTo("uk_ai_agent_call_tenant_code_active");
            assertThat(indexes.next()).isTrue();
            assertThat(indexes.getString(1)).isEqualTo("uk_ai_agent_message_invocation_role_active");
            assertThat(indexes.next()).isTrue();
            assertThat(indexes.getString(1)).isEqualTo("uk_ai_agent_message_session_sequence_active");
            assertThat(indexes.next()).isFalse();
        }
    }

    @Test
    void reusedInvocationIdShouldBeRejectedByPostgresAuthorityConstraint() throws Exception {
        try (Connection connection = connection()) {
            execute(connection, """
                    INSERT INTO public.ai_agent_message_record (
                        id, deleted, version, tenant_id, invocation_id, session_id,
                        sequence_no, role, content
                    ) VALUES (
                        'idempotency-user-1', false, 0, 'tenant-1', 'shared-invocation',
                        'session-1', 100, 'user', 'question-1'
                    )
                    """);
            insertCall(connection, "call-1", "idempotency-user-1", 100);
            execute(connection, """
                    INSERT INTO public.ai_agent_message_record (
                        id, deleted, version, tenant_id, invocation_id, session_id,
                        sequence_no, role, content
                    ) VALUES (
                        'idempotency-user-2', false, 0, 'tenant-1', 'shared-invocation-2',
                        'session-1', 102, 'user', 'question-2'
                    )
                    """);

            assertThatThrownBy(() -> insertCall(connection, "call-2", "idempotency-user-2", 102))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("uk_ai_agent_call_tenant_code_active");
        }
    }

    private static boolean reserve(
            String invocationId,
            String messageId,
            CountDownLatch started,
            CountDownLatch waitBeforeLock,
            CountDownLatch waitBeforeCommit) throws Exception {
        if (waitBeforeLock != null) {
            assertThat(waitBeforeLock.await(10, TimeUnit.SECONDS)).isTrue();
        }
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            started.countDown();
            try (var lock = connection.prepareStatement("""
                    SELECT active_invocation_id, message_count
                    FROM public.ai_agent_session_record
                    WHERE tenant_id = 'tenant-1' AND id = 'session-1'
                    FOR UPDATE
                    """)) {
                try (ResultSet session = lock.executeQuery()) {
                    assertThat(session.next()).isTrue();
                    if (session.getString("active_invocation_id") != null) {
                        connection.rollback();
                        return false;
                    }
                    int userSequence = session.getInt("message_count") + 1;
                    try (var insert = connection.prepareStatement("""
                            INSERT INTO public.ai_agent_message_record (
                                id, deleted, version, tenant_id, invocation_id, session_id,
                                sequence_no, role, content
                            ) VALUES (?, false, 0, 'tenant-1', ?, 'session-1', ?, 'user', 'question')
                            """);
                        var update = connection.prepareStatement("""
                                UPDATE public.ai_agent_session_record
                                SET active_invocation_id = ?, message_count = ?
                                WHERE tenant_id = 'tenant-1' AND id = 'session-1'
                                """)) {
                        insert.setString(1, messageId);
                        insert.setString(2, invocationId);
                        insert.setInt(3, userSequence);
                        insert.executeUpdate();
                        update.setString(1, invocationId);
                        update.setInt(2, userSequence + 1);
                        update.executeUpdate();
                    }
                }
            }
            if (waitBeforeCommit != null) {
                assertThat(waitBeforeCommit.await(10, TimeUnit.SECONDS)).isTrue();
            }
            connection.commit();
            return true;
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void insertCall(
            Connection connection, String callId, String userMessageId, int userSequence)
            throws SQLException {
        try (var insert = connection.prepareStatement("""
                INSERT INTO public.ai_agent_call_record (
                    id, deleted, version, enabled, tenant_id, invocation_code, request_fingerprint,
                    execution_token, lease_expires_at, attempt_count, session_id, user_message_id,
                    reserved_user_sequence_no, reserved_assistant_sequence_no, agent_id, agent_code,
                    provider_type, operation, invocation_state
                ) VALUES (
                    ?, false, 0, true, 'tenant-1', 'shared-invocation',
                    '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef',
                    'execution-token', now() + interval '15 minutes', 1, 'session-1', ?,
                    ?, ?, 'agent-1', 'assistant', 'DIFY', 'CHAT', 'RUNNING'
                )
                """)) {
            insert.setString(1, callId);
            insert.setString(2, userMessageId);
            insert.setInt(3, userSequence);
            insert.setInt(4, userSequence + 1);
            insert.executeUpdate();
        }
    }

    private static String createTable(String baseline, String tableName) {
        return statement(baseline, "CREATE TABLE public." + tableName);
    }

    private static String statement(String baseline, String startText) {
        int start = baseline.indexOf(startText);
        if (start < 0) throw new IllegalStateException("数据库基线缺少语句: " + startText);
        int end = baseline.indexOf(';', start);
        if (end < 0) throw new IllegalStateException("数据库基线语句未结束: " + startText);
        return baseline.substring(start, end + 1);
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        if (current == null) throw new IllegalStateException("未找到 ai-platform 仓库根目录");
        return current;
    }
}
