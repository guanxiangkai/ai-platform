package com.ai.files.service;

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

/** 使用真实 PostgreSQL 验证节点上传互斥与版本号分配边界。 */
@Testcontainers
class FileUploadPostgresConcurrencyTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:latest");

    @BeforeAll
    static void createSchema() throws Exception {
        String baseline = Files.readString(repositoryRoot()
                .resolve("deploy/database/V001__create_platform_schema.sql"));
        try (Connection connection = connection()) {
            execute(connection, statement(baseline, "CREATE TABLE public.file_space"));
            execute(connection, statement(baseline, "CREATE TABLE public.file_node"));
            execute(connection, statement(baseline, "CREATE TABLE public.file_version"));
            execute(connection, statement(baseline, "CREATE TABLE public.file_upload_record"));
            execute(connection, statement(baseline, "CREATE UNIQUE INDEX uk_file_version_no_active"));
            execute(connection, statement(baseline, "CREATE UNIQUE INDEX uk_file_upload_node_open"));
            execute(connection, """
                    INSERT INTO public.file_space (
                        id, deleted, version, tenant_id, enabled, space_code, space_name, space_type,
                        owner_user_id, quota_bytes, used_bytes, reserved_bytes
                    ) VALUES (
                        'space-1', false, 0, 'tenant-1', true, 'personal-user-1', '我的文件',
                        'PERSONAL', 'user-1', 1024, 10, 0
                    )
                    """);
            execute(connection, """
                    INSERT INTO public.file_node (
                        id, deleted, version, tenant_id, enabled, space_id, node_type, node_name,
                        display_path, node_state
                    ) VALUES (
                        'node-1', false, 0, 'tenant-1', true, 'space-1', 'FILE',
                        'report.txt', '/report.txt', 'ACTIVE'
                    )
                    """);
            execute(connection, """
                    INSERT INTO public.file_version (
                        id, deleted, version, tenant_id, enabled, node_id, version_no, version_state,
                        object_key, original_name, content_type, size_bytes, sha256
                    ) VALUES (
                        'version-1', false, 0, 'tenant-1', true, 'node-1', 1, 'AVAILABLE',
                        'object-1', 'report.txt', 'text/plain', 10,
                        '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef'
                    )
                    """);
        }
    }

    @Test
    void concurrentReservationsShouldAllowOnlyOneActiveUploadAndOneNextVersion() throws Exception {
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> reserve(
                    "upload-1", firstLocked, null, allowFirstCommit));
            assertThat(firstLocked.await(10, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> reserve(
                    "upload-2", secondStarted, firstLocked, null));
            assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
            allowFirstCommit.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(10, TimeUnit.SECONDS)).isFalse();
        }

        try (Connection connection = connection();
             var statement = connection.createStatement();
             ResultSet versions = statement.executeQuery("""
                     SELECT version_no
                     FROM public.file_version
                     WHERE tenant_id = 'tenant-1' AND node_id = 'node-1'
                     ORDER BY version_no
                     """)) {
            assertThat(versions.next()).isTrue();
            assertThat(versions.getInt(1)).isEqualTo(1);
            assertThat(versions.next()).isTrue();
            assertThat(versions.getInt(1)).isEqualTo(2);
            assertThat(versions.next()).isFalse();
        }
        try (Connection connection = connection();
             var statement = connection.createStatement();
             ResultSet space = statement.executeQuery("""
                     SELECT reserved_bytes FROM public.file_space
                     WHERE tenant_id = 'tenant-1' AND id = 'space-1'
                     """)) {
            assertThat(space.next()).isTrue();
            assertThat(space.getLong(1)).isEqualTo(10L);
        }
    }

    private static boolean reserve(
            String uploadId,
            CountDownLatch started,
            CountDownLatch waitBeforeLock,
            CountDownLatch waitBeforeCommit) throws Exception {
        if (waitBeforeLock != null) {
            assertThat(waitBeforeLock.await(10, TimeUnit.SECONDS)).isTrue();
        }
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            started.countDown();
            lockSpace(connection);
            try (var lock = connection.prepareStatement("""
                    SELECT active_upload_id
                    FROM public.file_node
                    WHERE tenant_id = 'tenant-1' AND id = 'node-1'
                    FOR UPDATE
                    """)) {
                try (ResultSet node = lock.executeQuery()) {
                    assertThat(node.next()).isTrue();
                    if (node.getString("active_upload_id") != null) {
                        connection.rollback();
                        return false;
                    }
                }
            }
            int versionNo = nextVersionNo(connection);
            insertReservation(connection, uploadId, versionNo);
            if (waitBeforeCommit != null) {
                assertThat(waitBeforeCommit.await(10, TimeUnit.SECONDS)).isTrue();
            }
            connection.commit();
            return true;
        }
    }

    private static void lockSpace(Connection connection) throws SQLException {
        try (var lock = connection.prepareStatement("""
                SELECT reserved_bytes
                FROM public.file_space
                WHERE tenant_id = 'tenant-1' AND id = 'space-1'
                FOR UPDATE
                """)) {
            try (ResultSet space = lock.executeQuery()) {
                assertThat(space.next()).isTrue();
            }
        }
    }

    private static int nextVersionNo(Connection connection) throws SQLException {
        try (var query = connection.prepareStatement("""
                SELECT COALESCE(max(version_no), 0) + 1
                FROM public.file_version
                WHERE tenant_id = 'tenant-1' AND node_id = 'node-1'
                """)) {
            try (ResultSet result = query.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getInt(1);
            }
        }
    }

    private static void insertReservation(Connection connection, String uploadId, int versionNo)
            throws SQLException {
        String versionId = "version-" + uploadId;
        try (var version = connection.prepareStatement("""
                INSERT INTO public.file_version (
                    id, deleted, version, tenant_id, enabled, node_id, version_no, version_state,
                    object_key, original_name, content_type, size_bytes, sha256
                ) VALUES (
                    ?, false, 0, 'tenant-1', true, 'node-1', ?, 'PENDING', ?,
                    'report.txt', 'text/plain', 10,
                    '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef'
                )
                """);
             var node = connection.prepareStatement("""
                UPDATE public.file_node SET active_upload_id = ?
                WHERE tenant_id = 'tenant-1' AND id = 'node-1'
                """);
             var space = connection.prepareStatement("""
                UPDATE public.file_space SET reserved_bytes = reserved_bytes + 10
                WHERE tenant_id = 'tenant-1' AND id = 'space-1'
                """);
             var upload = connection.prepareStatement("""
                INSERT INTO public.file_upload_record (
                    id, deleted, version, tenant_id, enabled, space_id, node_id, version_id,
                    object_key, operator_user_id, reserved_bytes, new_node, upload_state,
                    execution_token, lease_expires_at, next_attempt_at, attempt_count
                ) VALUES (
                    ?, false, 0, 'tenant-1', true, 'space-1', 'node-1', ?, ?, 'user-1',
                    10, false, 'PREPARED', 'execution-token', now() + interval '45 minutes', now(), 1
                )
                """)) {
            version.setString(1, versionId);
            version.setInt(2, versionNo);
            version.setString(3, "object-" + uploadId);
            version.executeUpdate();
            node.setString(1, uploadId);
            node.executeUpdate();
            space.executeUpdate();
            upload.setString(1, uploadId);
            upload.setString(2, versionId);
            upload.setString(3, "object-" + uploadId);
            upload.executeUpdate();
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
