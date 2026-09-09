package com.ai.files.service;

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

/** 使用真实 PostgreSQL 验证节点上传互斥与版本号分配边界。 */
@Testcontainers
class FileUploadPostgresConcurrencyTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4");

    @BeforeAll
    static void createSchema() throws Exception {
        try (Connection connection = connection()) {
            execute(connection, """
                    CREATE TABLE public.file_space (
                        id varchar(64) PRIMARY KEY,
                        deleted boolean NOT NULL,
                        version bigint NOT NULL,
                        tenant_id varchar(64) NOT NULL,
                        enabled boolean NOT NULL,
                        space_code varchar(128) NOT NULL,
                        space_name varchar(128) NOT NULL,
                        space_type varchar(32) NOT NULL,
                        owner_user_id varchar(64),
                        quota_bytes bigint NOT NULL,
                        used_bytes bigint NOT NULL,
                        reserved_bytes bigint NOT NULL,
                        CONSTRAINT uk_file_space_tenant_code UNIQUE (tenant_id, space_code)
                    )
                    """);
            execute(connection, """
                    CREATE TABLE public.file_node (
                        id varchar(64) PRIMARY KEY,
                        deleted boolean NOT NULL,
                        version bigint NOT NULL,
                        tenant_id varchar(64) NOT NULL,
                        enabled boolean NOT NULL,
                        space_id varchar(64) NOT NULL,
                        node_type varchar(32) NOT NULL,
                        node_name varchar(255) NOT NULL,
                        display_path text NOT NULL,
                        node_state varchar(32) NOT NULL,
                        active_upload_id varchar(64)
                    )
                    """);
            execute(connection, """
                    CREATE TABLE public.file_version (
                        id varchar(64) PRIMARY KEY,
                        deleted boolean NOT NULL,
                        version bigint NOT NULL,
                        tenant_id varchar(64) NOT NULL,
                        enabled boolean NOT NULL,
                        node_id varchar(64) NOT NULL,
                        version_no integer NOT NULL,
                        version_state varchar(32) NOT NULL,
                        object_key varchar(512) NOT NULL,
                        original_name varchar(255) NOT NULL,
                        content_type varchar(255),
                        size_bytes bigint NOT NULL,
                        sha256 char(64) NOT NULL
                    )
                    """);
            execute(connection, """
                    CREATE TABLE public.file_upload_record (
                        id varchar(64) PRIMARY KEY,
                        deleted boolean NOT NULL,
                        version bigint NOT NULL,
                        tenant_id varchar(64) NOT NULL,
                        enabled boolean NOT NULL,
                        space_id varchar(64) NOT NULL,
                        node_id varchar(64) NOT NULL,
                        version_id varchar(64) NOT NULL,
                        object_key varchar(512) NOT NULL,
                        operator_user_id varchar(64) NOT NULL,
                        reserved_bytes bigint NOT NULL,
                        new_node boolean NOT NULL,
                        upload_state varchar(32) NOT NULL,
                        execution_token varchar(128) NOT NULL,
                        lease_expires_at timestamptz NOT NULL,
                        next_attempt_at timestamptz NOT NULL,
                        attempt_count integer NOT NULL
                    )
                    """);
            execute(connection, """
                    CREATE UNIQUE INDEX uk_file_version_no_active
                    ON public.file_version (tenant_id, node_id, version_no)
                    WHERE deleted = false
                    """);
            execute(connection, """
                    CREATE UNIQUE INDEX uk_file_upload_node_open
                    ON public.file_upload_record (tenant_id, node_id)
                    WHERE deleted = false AND upload_state = 'PREPARED'
                    """);
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
    void firstSystemSpaceCreationShouldBeUniqueWithinEachTenant() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> insertTenantSystemSpace("root-tenant-a", "root-space-a1", ready));
            var second = executor.submit(() -> insertTenantSystemSpace("root-tenant-a", "root-space-a2", ready));
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(second.get(10, TimeUnit.SECONDS));
        }
        String other = insertTenantSystemSpace("root-tenant-b", "root-space-b", new CountDownLatch(0));
        assertThat(other).isEqualTo("root-space-b");
        try (Connection connection = connection(); var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT count(*) FROM file_space WHERE space_code='system'")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(2);
        }
    }

    /** 独立验证部署所需数据库唯一边界；服务冲突后重查由业务服务测试覆盖。 */
    private static String insertTenantSystemSpace(String tenant, String candidate, CountDownLatch ready) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            ready.countDown();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            try (var statement = connection.prepareStatement("""
                    INSERT INTO file_space(id,deleted,version,tenant_id,enabled,space_code,space_name,space_type,quota_bytes,used_bytes,reserved_bytes)
                    VALUES (?,false,0,?,true,'system','Business files','SYSTEM',100,0,0)
                    """)) {
                statement.setString(1, candidate); statement.setString(2, tenant); statement.executeUpdate();
                connection.commit();
            } catch (SQLException conflict) {
                connection.rollback();
                if (!"23505".equals(conflict.getSQLState())) throw conflict;
            }
            try (var query = connection.prepareStatement("SELECT id FROM file_space WHERE tenant_id=? AND space_code='system'")) {
                query.setString(1, tenant);
                try (ResultSet result = query.executeQuery()) {
                    assertThat(result.next()).isTrue(); String id = result.getString(1);
                    assertThat(result.next()).isFalse(); return id;
                }
            }
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

}
