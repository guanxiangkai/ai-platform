package com.ai.system.domain.entity;

import com.ai.system.domain.MenuType;
import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
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
 * 菜单实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_menu", comment = "菜单表")
public class Menu extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "parent_id", length = 64, comment = "父菜单ID")
    private String parentId;

    @Column(name = "menu_name", nullable = false, length = 128, comment = "菜单名称")
    private String menuName;

    @Column(name = "menu_title", length = 128, comment = "菜单标题")
    private String menuTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "menu_type", nullable = false, length = 20, comment = "菜单类型")
    private MenuType menuType;

    @Column(name = "path", length = 255, comment = "路由路径")
    private String path;

    @Column(name = "component", length = 255, comment = "前端组件注册键")
    private String component;

    @Column(name = "permission", length = 128, comment = "权限标识")
    private String permission;

    @Column(name = "icon", length = 128, comment = "图标")
    private String icon;

    @Column(name = "visible", nullable = false, columnDefinition = "boolean default true", comment = "是否可见")
    private Boolean visible = true;

    @Column(name = "keep_alive", nullable = false, columnDefinition = "boolean default false", comment = "是否缓存")
    private Boolean keepAlive = false;

    @Column(name = "is_external", nullable = false, columnDefinition = "boolean default false", comment = "是否外链")
    private Boolean isExternal = false;
}
