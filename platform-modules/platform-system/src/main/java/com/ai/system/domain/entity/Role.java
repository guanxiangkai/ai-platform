package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 角色实体
 * <p>
 * 角色定义用户的功能权限（可以做什么操作）
 * 数据权限（可以访问哪些数据）由岗位控制
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_role", comment = "角色表")
public class Role extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "role_code", nullable = false, length = 64, comment = "角色编码")
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 128, comment = "角色名称")
    private String roleName;

    @Column(name = "data_scope", comment = "数据权限范围")
    private Integer dataScope;

    /**
     * 是否为默认注册角色
     * <p>
     * 审核通过时，若无法通过"部门名称+职位名称"或"职位名称"匹配到合适角色，
     * 则自动分配标记了此字段的角色作为兜底默认角色。
     * </p>
     */
    @Column(name = "default_registration_role", nullable = false,
            columnDefinition = "boolean default false",
            comment = "是否为默认注册角色(注册审核兜底使用)")
    private Boolean defaultRegistrationRole = false;
}
