package com.ai.system.security;

import com.ai.api.security.PlatformSuperAdmin;
import com.ai.system.domain.entity.Menu;
import com.ai.system.domain.vo.MenuVO;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;

/** 租户目录只对固定平台超级管理员可见。 */
public final class TenantMenuVisibility {

    private TenantMenuVisibility() {
    }

    /** 只有安全上下文中的固定超级管理员身份拥有租户管理权。 */
    public static boolean isPlatformSuperAdmin() {
        return SecurityUtils.isSuperAdmin() && PlatformSuperAdmin.USER_ID.equals(SecurityUtils.getUserId());
    }

    /** 使用稳定权限、路径和组件键识别租户管理节点，不依赖显示文案。 */
    public static boolean isTenantNode(Menu menu) {
        return menu != null && (tenantPermission(menu.getPermission())
                || tenantPath(menu.getPath()) || tenantComponent(menu.getComponent()));
    }

    /** 读取模型采用与实体一致的分类规则。 */
    public static boolean isTenantNode(MenuVO menu) {
        return menu != null && (tenantPermission(menu.getPermission())
                || tenantPath(menu.getPath()) || tenantComponent(menu.getComponent()));
    }

    /** 计算租户节点及所有后代，供分页 SQL、详情与写入边界复用同一集合。 */
    public static Set<String> hiddenIds(Collection<Menu> menus) {
        return excludedNodes(menus.stream().filter(java.util.Objects::nonNull)
                .map(menu -> new Node(menu.getId(), menu.getParentId(), isTenantNode(menu))).toList());
    }

    /** 过滤平面菜单集合后再由服务组树，避免隐藏目录的后代成为孤立入口。 */
    public static List<MenuVO> filterTree(Collection<MenuVO> menus) {
        if (isPlatformSuperAdmin() || menus == null || menus.isEmpty()) return menus == null ? List.of() : List.copyOf(menus);
        Set<String> hidden = excludedNodes(menus.stream().filter(java.util.Objects::nonNull)
                .map(menu -> new Node(menu.getId(), menu.getParentId(), isTenantNode(menu))).toList());
        return menus.stream().filter(menu -> menu != null && !hidden.contains(menu.getId())).toList();
    }

    private record Node(String id, String parentId, boolean tenant) { }

    private static Set<String> excludedNodes(List<Node> nodes) {
        Set<String> hidden = new HashSet<>();
        Map<String, List<String>> children = new HashMap<>();
        ArrayDeque<String> pending = new ArrayDeque<>();
        for (Node node : nodes) {
            if (node.id() == null) continue;
            if (node.tenant()) pending.add(node.id());
            if (node.parentId() != null) children.computeIfAbsent(node.parentId(), ignored -> new ArrayList<>()).add(node.id());
        }
        while (!pending.isEmpty()) {
            String id = pending.removeFirst();
            if (hidden.add(id)) pending.addAll(children.getOrDefault(id, List.of()));
        }
        return Set.copyOf(hidden);
    }

    private static boolean tenantPermission(String value) {
        if (value != null) value = value.trim();
        return value != null && (value.equals("system:tenant") || value.startsWith("system:tenant:"));
    }

    private static boolean tenantPath(String value) {
        if (value != null) value = value.trim();
        return value != null && (value.equals("/system/tenant") || value.startsWith("/system/tenant/")
                || value.equals("/system/tenants") || value.startsWith("/system/tenants/")
                || value.equals("/tenants") || value.startsWith("/tenants/"));
    }

    private static boolean tenantComponent(String value) {
        if (value != null) value = value.trim();
        return value != null && (value.equals("admin/TenantManagementPage")
                || value.equals("TenantManagementPage") || value.startsWith("admin/TenantManagementPage/"));
    }
}
