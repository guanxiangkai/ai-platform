package com.ai.files.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;

/** 一个用户的一份确定原件上传目标；ID即暂存目录ID，至多接收一个成功版本。 */
@Getter @Setter @Entity
@Table(name="file_browser_upload_target", uniqueConstraints=@UniqueConstraint(name="uq_browser_upload_business", columnNames={"tenant_id","business_type","business_id"}))
public class FileBrowserUploadTarget extends DataTenantEntity {
    @Column(name="space_id",nullable=false,length=64) private String spaceId;
    @Column(name="staging_parent_id",nullable=false,length=64) private String stagingParentId;
    @Column(name="business_type",nullable=false,length=64) private String businessType;
    @Column(name="business_id",nullable=false,length=128) private String businessId;
    @Column(name="uploader_user_id",nullable=false,length=64) private String uploaderUserId;
    @Column(name="filename",nullable=false,length=256) private String filename;
    @Column(name="size_bytes",nullable=false) private long sizeBytes;
    @Column(name="sha256",nullable=false,length=64) private String sha256;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="file_id",length=64) private String fileId;
    @Column(name="version_id",length=64) private String versionId;
    @Column(name="accepted_parent_id",length=64) private String acceptedParentId;
}
