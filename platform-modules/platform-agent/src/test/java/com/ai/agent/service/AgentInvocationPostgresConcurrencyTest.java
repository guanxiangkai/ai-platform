package com.ai.agent.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

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
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4");

    @BeforeAll
    static void createAgentInvocationSchema() throws Exception {
        try (Connection connection = connection()) {
            execute(connection, """
                    CREATE TABLE public.ai_agent_config (
                        tenant_id varchar(64) NOT NULL,
                        id varchar(64) NOT NULL,
                        CONSTRAINT uk_agent_config_tenant_id UNIQUE (tenant_id, id)
                    )
                    """);
            execute(connection, """
                    CREATE TABLE public.ai_agent_session_record (
                        id varchar(64) PRIMARY KEY,
                        deleted boolean NOT NULL,
                        version bigint NOT NULL,
                        enabled boolean NOT NULL,
                        tenant_id varchar(64) NOT NULL,
                        session_code varchar(128) NOT NULL,
                        agent_id varchar(64) NOT NULL,
                        agent_code varchar(128) NOT NULL,
                        user_id varchar(64) NOT NULL,
                        message_count integer NOT NULL,
                        session_state varchar(32) NOT NULL,
                        started_at timestamptz NOT NULL,
                        active_invocation_id varchar(128)
                    )
                    """);
            execute(connection, """
                    CREATE TABLE public.ai_agent_message_record (
                        id varchar(64) PRIMARY KEY,
                        deleted boolean NOT NULL,
                        version bigint NOT NULL,
                        tenant_id varchar(64) NOT NULL,
                        invocation_id varchar(128) NOT NULL,
                        session_id varchar(64) NOT NULL,
                        sequence_no integer NOT NULL,
                        role varchar(32) NOT NULL,
                        content text NOT NULL
                    )
                    """);
            execute(connection, """
                    CREATE TABLE public.ai_agent_call_record (
                        id varchar(64) PRIMARY KEY,
                        deleted boolean NOT NULL,
                        version bigint NOT NULL,
                        enabled boolean NOT NULL,
                        tenant_id varchar(64) NOT NULL,
                        invocation_code varchar(128) NOT NULL,
                        request_fingerprint varchar(128) NOT NULL,
                        execution_token varchar(128) NOT NULL,
                        lease_expires_at timestamptz NOT NULL,
                        attempt_count integer NOT NULL,
                        session_id varchar(64) NOT NULL,
                        user_message_id varchar(64) NOT NULL,
                        reserved_user_sequence_no integer NOT NULL,
                        reserved_assistant_sequence_no integer NOT NULL,
                        agent_id varchar(64) NOT NULL,
                        agent_code varchar(128) NOT NULL,
                        provider_type varchar(32) NOT NULL,
                        operation varchar(32) NOT NULL,
                        invocation_state varchar(32) NOT NULL
                    )
                    """);
            execute(connection, """
                    CREATE UNIQUE INDEX uk_ai_agent_message_session_sequence_active
                    ON public.ai_agent_message_record (tenant_id, session_id, sequence_no)
                    WHERE deleted = false
                    """);
            execute(connection, """
                    CREATE UNIQUE INDEX uk_ai_agent_message_invocation_role_active
                    ON public.ai_agent_message_record (tenant_id, invocation_id, role)
                    WHERE deleted = false
                    """);
            execute(connection, """
                    CREATE UNIQUE INDEX uk_ai_agent_call_tenant_code_active
                    ON public.ai_agent_call_record (tenant_id, invocation_code)
                    WHERE deleted = false
                    """);
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
    void schemaShouldExposeDatabaseIdempotencyAndSequenceConstraints() throws Exception {
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

}
