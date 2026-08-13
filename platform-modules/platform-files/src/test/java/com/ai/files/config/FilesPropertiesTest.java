package com.ai.files.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 文件容量与时效配置契约测试。 */
class FilesPropertiesTest {

    @Test
    void missingValuesShouldUseSafeDefaults() {
        FilesProperties properties = properties(
                null, null, null, null,
                null, null, null, null, null, null, null);

        assertEquals(10L * 1024 * 1024 * 1024, properties.personalQuotaBytes());
        assertEquals(100L * 1024 * 1024 * 1024, properties.departmentQuotaBytes());
        assertEquals(100L * 1024 * 1024 * 1024, properties.systemQuotaBytes());
        assertEquals(2L * 1024 * 1024 * 1024, properties.resolvedMaxFileSizeBytes());
        assertEquals(Duration.ofMinutes(10), properties.resolvedAccessUrlTtl());
        assertEquals(Duration.ofMinutes(30), properties.resolvedEditLockTtl());
        assertEquals(Duration.ofMinutes(30), properties.resolvedObjectOperationTimeout());
        assertEquals(Duration.ofMinutes(45), properties.resolvedUploadLease());
        assertEquals(Duration.ofMinutes(1), properties.resolvedReconcileRetryDelay());
        assertEquals(Duration.ofSeconds(30), properties.resolvedReconcileInterval());
        assertEquals(50, properties.resolvedReconcileBatchSize());
    }

    @Test
    void explicitNonPositiveQuotaAndFileSizeMustFail() {
        assertThrows(IllegalArgumentException.class, () -> properties(
                0L, null, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, -1L, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, 0L, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, null, -1L, null, null, null, null, null, null, null));
    }

    @Test
    void explicitNonPositiveDurationsMustFail() {
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, null, null, Duration.ZERO, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, null, null, null, Duration.ofSeconds(-1), null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, null, null, null, null, Duration.ZERO, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, null, null, null, null, null, Duration.ofSeconds(-1), null, null, null));
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, null, null, null, null, null, null, Duration.ZERO, null, null));
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, null, null, null, null, null, null, null, Duration.ofSeconds(-1), null));
    }

    @Test
    void reconcileBatchSizeMustStayWithinConfiguredBounds() {
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, null, null, null, null, null, null, null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, null, null, null, null, null, null, null, null, 1_001));
    }

    @Test
    void uploadLeaseMustBeLongerThanObjectOperationTimeout() {
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, null, null, null, null,
                Duration.ofMinutes(30), Duration.ofMinutes(30), null, null, null));
        assertThrows(IllegalArgumentException.class, () -> properties(
                null, null, null, null, null, null,
                Duration.ofMinutes(30), Duration.ofMinutes(29), null, null, null));
    }

    private FilesProperties properties(
            Long personalQuota,
            Long departmentQuota,
            Long systemQuota,
            Long maxFileSize,
            Duration accessUrlTtl,
            Duration editLockTtl,
            Duration objectOperationTimeout,
            Duration uploadLease,
            Duration reconcileRetryDelay,
            Duration reconcileInterval,
            Integer reconcileBatchSize
    ) {
        return new FilesProperties(
                personalQuota,
                departmentQuota,
                systemQuota,
                maxFileSize,
                accessUrlTtl,
                editLockTtl,
                objectOperationTimeout,
                uploadLease,
                reconcileRetryDelay,
                reconcileInterval,
                reconcileBatchSize);
    }
}
