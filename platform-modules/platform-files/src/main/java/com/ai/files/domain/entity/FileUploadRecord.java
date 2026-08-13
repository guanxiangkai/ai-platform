package com.ai.files.domain.entity;

import com.ai.files.domain.FileUploadState;
import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 连接数据库元数据与对象存储操作的可恢复上传记录。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "file_upload_record", comment = "文件上传可恢复状态表")
public class FileUploadRecord extends DataTenantEntity {

    @Column(name = "space_id", nullable = false, length = 64)
    private String spaceId;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;

    @Column(name = "version_id", nullable = false, length = 64)
    private String versionId;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "operator_user_id", nullable = false, length = 64)
    private String operatorUserId;

    @Column(name = "business_type", length = 64)
    private String businessType;

    @Column(name = "business_id", length = 128)
    private String businessId;

    @Column(name = "reserved_bytes", nullable = false)
    private Long reservedBytes;

    @Column(name = "new_node", nullable = false)
    private Boolean newNode;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_state", nullable = false, length = 24)
    private FileUploadState uploadState = FileUploadState.PREPARED;

    @Column(name = "execution_token", nullable = false, length = 64)
    private String executionToken;

    @Column(name = "lease_expires_at", nullable = false)
    private Instant leaseExpiresAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 1;

    @Column(name = "last_error", length = 2000)
    private String lastError;
}
