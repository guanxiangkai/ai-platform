package com.ai.system.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** 使用真实 PostgreSQL 验证注册目录主体部分唯一索引。 */
@Testcontainers
class RegisterPostgresConcurrencyTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4");

    @BeforeAll
    static void createSchema() throws SQLException {
        try (Connection connection = connection()) {
            execute(connection, """
                    CREATE TABLE public.sys_register (
                        id varchar(64) PRIMARY KEY,
                        tenant_id varchar(64) NOT NULL,
                        directory_subject_id varchar(64) NOT NULL,
                        user_id varchar(64),
                        deleted boolean NOT NULL,
                        registration_state varchar(32) NOT NULL
                    )
                    """);
            execute(connection, """
                    CREATE UNIQUE INDEX uk_sys_register_directory_subject_active
                    ON public.sys_register (tenant_id, directory_subject_id)
                    WHERE deleted = false AND registration_state <> 'REJECTED'
                    """);
            execute(connection, """
                    CREATE UNIQUE INDEX uk_sys_register_user_active
                    ON public.sys_register (tenant_id, user_id)
                    WHERE deleted = false AND user_id IS NOT NULL
                    """);
        }
    }

    @Test
    void concurrentActiveRegistrationsShouldLeaveOnlyOneDirectorySubjectOwner() throws Exception {
        CountDownLatch firstInserted = new CountDownLatch(1);
        CountDownLatch secondAttempted = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> insertThenCommit("concurrent-1", "tenant-a", "subject-1",
                    firstInserted, null, allowFirstCommit));
            assertThat(firstInserted.await(10, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> insertThenCommit("concurrent-2", "tenant-a", "subject-1",
                    null, secondAttempted, null));
            assertThat(secondAttempted.await(10, TimeUnit.SECONDS)).isTrue();
            allowFirstCommit.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(10, TimeUnit.SECONDS)).isFalse();
        }
        assertThat(countActive("tenant-a", "subject-1")).isEqualTo(1);
    }

    @Test
    void rejectedRegistrationShouldNotPreventResubmission() throws SQLException {
        assertThat(insert("rejected-1", "tenant-a", "subject-rejected", "REJECTED")).isTrue();

        assertThat(insert("rejected-2", "tenant-a", "subject-rejected", "PENDING")).isTrue();
    }

    @Test
    void softDeletedRegistrationShouldNotPreventResubmission() throws SQLException {
        assertThat(insert("deleted-1", "tenant-a", "subject-deleted", "PENDING")).isTrue();
        try (Connection connection = connection()) {
            execute(connection, "UPDATE public.sys_register SET deleted = true WHERE id = 'deleted-1'");
        }

        assertThat(insert("deleted-2", "tenant-a", "subject-deleted", "PENDING")).isTrue();
        assertThat(countActive("tenant-a", "subject-deleted")).isEqualTo(1);
    }

    @Test
    void sameDirectorySubjectShouldRemainIndependentAcrossTenants() throws SQLException {
        assertThat(insert("tenant-a-1", "tenant-a", "subject-shared", "PENDING")).isTrue();

        assertThat(insert("tenant-b-1", "tenant-b", "subject-shared", "PENDING")).isTrue();
    }

    @Test
    void sameTenantUserShouldNotBeLinkedToTwoDirectorySubjects() throws SQLException {
        assertThat(insert("user-1", "tenant-a", "subject-user-1", "PENDING", "local-user-1")).isTrue();

        assertThat(insert("user-2", "tenant-a", "subject-user-2", "PENDING", "local-user-1")).isFalse();
    }

    private static boolean insertThenCommit(
            String id,
            String tenantId,
            String directorySubjectId,
            CountDownLatch inserted,
            CountDownLatch attempted,
            CountDownLatch allowCommit) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            if (attempted != null) {
                attempted.countDown();
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO public.sys_register (id, tenant_id, directory_subject_id, deleted, registration_state)
                    VALUES (?, ?, ?, false, 'PENDING')
                    """)) {
                statement.setString(1, id);
                statement.setString(2, tenantId);
                statement.setString(3, directorySubjectId);
                statement.executeUpdate();
            } catch (SQLException exception) {
                connection.rollback();
                if (isUniqueViolation(exception)) {
                    return false;
                }
                throw exception;
            }
            if (inserted != null) {
                inserted.countDown();
            }
            if (allowCommit != null) {
                assertThat(allowCommit.await(10, TimeUnit.SECONDS)).isTrue();
            }
            connection.commit();
            return true;
        }
    }

    private static boolean insert(String id, String tenantId, String directorySubjectId, String state) throws SQLException {
        return insert(id, tenantId, directorySubjectId, state, null);
    }

    private static boolean insert(String id, String tenantId, String directorySubjectId, String state, String userId)
            throws SQLException {
        try (Connection connection = connection();
             var statement = connection.prepareStatement("""
                     INSERT INTO public.sys_register (id, tenant_id, directory_subject_id, user_id, deleted, registration_state)
                     VALUES (?, ?, ?, ?, false, ?)
                     """)) {
            statement.setString(1, id);
            statement.setString(2, tenantId);
            statement.setString(3, directorySubjectId);
            statement.setString(4, userId);
            statement.setString(5, state);
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            if (isUniqueViolation(exception)) {
                return false;
            }
            throw exception;
        }
    }

    private static int countActive(String tenantId, String directorySubjectId) throws SQLException {
        try (Connection connection = connection();
             var statement = connection.prepareStatement("""
                     SELECT count(*) FROM public.sys_register
                     WHERE tenant_id = ? AND directory_subject_id = ? AND deleted = false
                     """)) {
            statement.setString(1, tenantId);
            statement.setString(2, directorySubjectId);
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getInt(1);
            }
        }
    }

    private static boolean isUniqueViolation(SQLException exception) {
        return "23505".equals(exception.getSQLState());
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
