package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.log.entity.BaseLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
/**
 * OSS 文件上传日志实体。
 *
 * <p>继承 {@link BaseLog} 复用租户、审计与日志公共字段，通过
 * {@code OssLogHandler} SPI 持久化。Web Plus 文件服务或 {@code @OssLog}
 * 标注的接口负责生成日志。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_oss_log", comment = "OSS文件上传日志表", indexes = {
        @Index(name = "idx_oss_log_user", columnList = "user_id"),
        @Index(name = "idx_oss_log_time", columnList = "log_time"),
        @Index(name = "idx_oss_log_module", columnList = "biz_module")
})
public class OssLog extends BaseLog {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属业务模块（如 PROFILE / KNOWLEDGE / DOCUMENT / TEMP 等）
     * <p>由调用方在 {@code @OssLog} 注解或上传接口中指定，便于按功能模块筛选附件</p>
     */
    @Column(name = "biz_module", length = 50, comment = "业务模块标识")
    private String bizModule;

    /**
     * 原始文件名
     */
    @Column(name = "original_name", length = 256, comment = "原始文件名")
    private String originalName;

    /**
     * MIME 类型
     */
    @Column(name = "content_type", length = 128, comment = "MIME类型")
    private String contentType;

    /**
     * 文件后缀
     */
    @Column(name = "file_suffix", length = 32, comment = "文件后缀")
    private String fileSuffix;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", comment = "文件大小(字节)")
    private Long fileSize;

    /**
     * 存储桶名称
     */
    @Column(name = "bucket_name", length = 128, comment = "存储桶名称")
    private String bucketName;

    /**
     * S3 对象键
     */
    @Column(name = "object_key", length = 512, comment = "S3对象键")
    private String objectKey;

    /**
     * 文件访问 URL
     */
    @Column(name = "file_url", length = 1024, comment = "文件访问URL")
    private String fileUrl;

    /** 文件内容 SHA-256。 */
    @Column(name = "file_hash", length = 64, comment = "文件SHA-256")
    private String fileHash;
}
