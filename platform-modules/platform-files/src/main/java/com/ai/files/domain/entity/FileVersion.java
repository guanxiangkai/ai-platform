package com.ai.files.domain.entity;

import com.ai.files.domain.FileVersionState;
import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 文件节点的一次不可变内容版本。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "file_version", comment = "文件版本表")
public class FileVersion extends DataTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属文件节点标识。 */
    @Column(name = "node_id", nullable = false, length = 64, comment = "文件节点标识")
    private String nodeId;

    /** 节点内递增的版本号。 */
    @Column(name = "version_no", nullable = false, comment = "节点内递增版本号")
    private Integer versionNo;

    /** 版本内容是否已可读。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "version_state", nullable = false, length = 16, comment = "文件版本状态")
    private FileVersionState versionState = FileVersionState.PENDING;

    /** S3 不可变对象键。 */
    @Column(name = "object_key", nullable = false, length = 1024, comment = "对象存储不可变对象键")
    private String objectKey;

    /** 上传时的原始文件名。 */
    @Column(name = "original_name", nullable = false, length = 256, comment = "上传时原始文件名")
    private String originalName;

    /** 媒体类型。 */
    @Column(name = "content_type", nullable = false, length = 128, comment = "文件媒体类型")
    private String contentType;

    /** 文件大小，单位为字节。 */
    @Column(name = "size_bytes", nullable = false, comment = "文件大小字节数")
    private Long sizeBytes;

    /** 文件内容 SHA-256。 */
    @Column(name = "sha256", nullable = false, length = 64, comment = "文件内容SHA-256哈希值")
    private String sha256;
}
