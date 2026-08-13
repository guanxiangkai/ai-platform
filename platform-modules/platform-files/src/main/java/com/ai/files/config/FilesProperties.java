package com.ai.files.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 平台文件服务的容量和时效配置。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "platform.files")
@Validated
public record FilesProperties(
        Long defaultPersonalQuotaBytes,
        Long defaultDepartmentQuotaBytes,
        Long defaultSystemQuotaBytes,
        Long maxFileSizeBytes,
        Duration accessUrlTtl,
        Duration editLockTtl,
        Duration objectOperationTimeout,
        Duration uploadLease,
        Duration reconcileRetryDelay,
        Duration reconcileInterval,
        Integer reconcileBatchSize
) {

    private static final long DEFAULT_PERSONAL_QUOTA_BYTES = 10L * 1024 * 1024 * 1024;
    private static final long DEFAULT_DEPARTMENT_QUOTA_BYTES = 100L * 1024 * 1024 * 1024;
    private static final long DEFAULT_SYSTEM_QUOTA_BYTES = 100L * 1024 * 1024 * 1024;
    private static final long DEFAULT_MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024 * 1024;
    private static final Duration DEFAULT_ACCESS_URL_TTL = Duration.ofMinutes(10);
    private static final Duration DEFAULT_EDIT_LOCK_TTL = Duration.ofMinutes(30);
    private static final Duration DEFAULT_OBJECT_OPERATION_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration DEFAULT_UPLOAD_LEASE = Duration.ofMinutes(45);
    private static final Duration DEFAULT_RECONCILE_RETRY_DELAY = Duration.ofMinutes(1);
    private static final Duration DEFAULT_RECONCILE_INTERVAL = Duration.ofSeconds(30);
    private static final int DEFAULT_RECONCILE_BATCH_SIZE = 50;

    /**
     * 仅为缺失配置补充安全默认值，并拒绝显式提供的非法值。
     *
     * @throws IllegalArgumentException 配额、时效、批量大小或租约关系不满足运行边界时抛出
     */
    public FilesProperties {
        defaultPersonalQuotaBytes = positiveOrDefault(
                defaultPersonalQuotaBytes, DEFAULT_PERSONAL_QUOTA_BYTES, "默认个人空间配额");
        defaultDepartmentQuotaBytes = positiveOrDefault(
                defaultDepartmentQuotaBytes, DEFAULT_DEPARTMENT_QUOTA_BYTES, "默认部门空间配额");
        defaultSystemQuotaBytes = positiveOrDefault(
                defaultSystemQuotaBytes, DEFAULT_SYSTEM_QUOTA_BYTES, "默认内部业务空间配额");
        maxFileSizeBytes = positiveOrDefault(maxFileSizeBytes, DEFAULT_MAX_FILE_SIZE_BYTES, "单文件大小上限");
        accessUrlTtl = positiveOrDefault(accessUrlTtl, DEFAULT_ACCESS_URL_TTL, "下载直链有效期");
        editLockTtl = positiveOrDefault(editLockTtl, DEFAULT_EDIT_LOCK_TTL, "编辑锁有效期");
        objectOperationTimeout = positiveOrDefault(
                objectOperationTimeout, DEFAULT_OBJECT_OPERATION_TIMEOUT, "对象存储操作超时");
        uploadLease = positiveOrDefault(uploadLease, DEFAULT_UPLOAD_LEASE, "上传执行权租约");
        reconcileRetryDelay = positiveOrDefault(
                reconcileRetryDelay, DEFAULT_RECONCILE_RETRY_DELAY, "上传恢复失败重试间隔");
        reconcileInterval = positiveOrDefault(reconcileInterval, DEFAULT_RECONCILE_INTERVAL, "未完成上传扫描间隔");
        reconcileBatchSize = reconcileBatchSize == null ? DEFAULT_RECONCILE_BATCH_SIZE : reconcileBatchSize;
        if (reconcileBatchSize < 1 || reconcileBatchSize > 1_000) {
            throw new IllegalArgumentException("上传恢复单次扫描数量必须在 1 到 1000 之间");
        }
        if (uploadLease.compareTo(objectOperationTimeout) <= 0) {
            throw new IllegalArgumentException("文件上传租约必须长于对象存储操作超时");
        }
    }

    /** 默认个人空间配额，10 GiB。 */
    public long personalQuotaBytes() {
        return defaultPersonalQuotaBytes;
    }

    /** 默认部门空间配额，100 GiB。 */
    public long departmentQuotaBytes() {
        return defaultDepartmentQuotaBytes;
    }

    /** 默认内部业务空间配额，100 GiB。 */
    public long systemQuotaBytes() {
        return defaultSystemQuotaBytes;
    }

    /** 单文件大小上限，默认 2 GiB。 */
    public long resolvedMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    /** 下载直链有效期，默认 10 分钟。 */
    public Duration resolvedAccessUrlTtl() {
        return accessUrlTtl;
    }

    /** 编辑锁有效期，默认 30 分钟。 */
    public Duration resolvedEditLockTtl() {
        return editLockTtl;
    }

    /** 单次对象存储操作的最长执行时间，默认 30 分钟。 */
    public Duration resolvedObjectOperationTimeout() {
        return objectOperationTimeout;
    }

    /** 在线上传执行权租约，默认 45 分钟。 */
    public Duration resolvedUploadLease() {
        return uploadLease;
    }

    /** 上传恢复失败后的重试间隔，默认 1 分钟。 */
    public Duration resolvedReconcileRetryDelay() {
        return reconcileRetryDelay;
    }

    /** 未完成上传的扫描间隔，默认 30 秒。 */
    public Duration resolvedReconcileInterval() {
        return reconcileInterval;
    }

    /** 单次扫描处理上限，默认 50 条。 */
    public int resolvedReconcileBatchSize() {
        return reconcileBatchSize;
    }

    private static long positiveOrDefault(Long value, long defaultValue, String name) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(name + "必须大于 0");
        }
        return value;
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue, String name) {
        if (value == null) {
            return defaultValue;
        }
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + "必须大于 0");
        }
        return value;
    }
}
