package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serial;

/**
 * 角色菜单关系实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_role_menu", comment = "角色菜单关系表")
public class RoleMenu extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @Column(name = "role_id", nullable = false, length = 64, comment = "角色ID")
    private String roleId;

    /**
     * 菜单ID
     */
    @Column(name = "menu_id", nullable = false, length = 64, comment = "菜单ID")
    private String menuId;
}
