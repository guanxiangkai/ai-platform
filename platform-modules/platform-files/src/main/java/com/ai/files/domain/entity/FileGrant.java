package com.ai.files.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import com.ai.files.domain.FilePrincipalType;
import com.ai.files.domain.FileRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.Instant;

/** 授予用户或部门的空间级、节点级访问权限。 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "file_grant", comment = "文件授权表")
public class FileGrant extends DataTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属空间标识。 */
    @Column(name = "space_id", nullable = false, length = 64, comment = "文件空间标识")
    private String spaceId;

    /** 节点标识；为空时表示整个空间。 */
    @Column(name = "node_id", length = 64, comment = "文件节点标识")
    private String nodeId;

    /** 授权主体类型。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false, length = 20, comment = "授权主体类型")
    private FilePrincipalType principalType;

    /** 用户或部门标识。 */
    @Column(name = "principal_id", nullable = false, length = 64, comment = "授权主体标识")
    private String principalId;

    /** 授权角色。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "grant_role", nullable = false, length = 20, comment = "授权角色")
    private FileRole grantRole;

    /** 节点级授权是否向子节点继承。 */
    @Column(name = "inherited", nullable = false, comment = "是否向子节点继承")
    private Boolean inherited = true;

    /** 授权过期时间；为空表示长期有效。 */
    @Column(name = "expires_at", comment = "授权过期时间")
    private Instant expiresAt;
}
