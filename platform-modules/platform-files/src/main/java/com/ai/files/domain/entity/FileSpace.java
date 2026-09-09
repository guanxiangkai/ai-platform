package com.ai.files.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import com.ai.files.domain.FileSpaceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 租户内的个人、部门或系统文件空间。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "file_space", comment = "文件空间表", uniqueConstraints =
        @UniqueConstraint(name = "uk_file_space_tenant_code", columnNames = {"tenant_id", "space_code"}))
public class FileSpace extends DataTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 空间稳定编码。 */
    @Column(name = "space_code", nullable = false, length = 128, comment = "文件空间编码")
    private String spaceCode;

    /** 空间显示名称。 */
    @Column(name = "space_name", nullable = false, length = 256, comment = "文件空间名称")
    private String spaceName;

    /** 空间类型。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "space_type", nullable = false, length = 20, comment = "文件空间类型")
    private FileSpaceType spaceType;

    /** 个人空间所有者；非个人空间为空。 */
    @Column(name = "owner_user_id", length = 64, comment = "个人空间所有者用户标识")
    private String ownerUserId;

    /** 部门空间所有者；非部门空间为空。 */
    @Column(name = "owner_dept_id", length = 64, comment = "部门空间所有者部门标识")
    private String ownerDeptId;

    /** 空间总配额，单位为字节。 */
    @Column(name = "quota_bytes", nullable = false, comment = "空间总配额字节数")
    private Long quotaBytes;

    /** 已用容量，单位为字节。 */
    @Column(name = "used_bytes", nullable = false, comment = "已用容量字节数")
    private Long usedBytes = 0L;

    /** 正在上传但尚未转为已用容量的预留字节数。 */
    @Column(name = "reserved_bytes", nullable = false, comment = "上传中预留容量字节数")
    private Long reservedBytes = 0L;
}
