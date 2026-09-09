package com.ai.system.security;

import com.ai.system.domain.MenuType;
import com.ai.system.domain.vo.MenuVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMenuVisibilityTest {

    @org.junit.jupiter.api.AfterEach
    void clearContext() { io.github.guanxiangkai.web.plus.core.context.CurrentUserHolder.clear(); }

    @Test
    void excludesPlainGrandchildrenOfPathOnlyTenantNodesWithoutHidingBasicAdministration() {
        var tenant = new com.ai.system.domain.entity.Menu(); tenant.setId("tenant"); tenant.setPath(" /system/tenants ");
        var child = new com.ai.system.domain.entity.Menu(); child.setId("child"); child.setParentId("tenant");
        var grandchild = new com.ai.system.domain.entity.Menu(); grandchild.setId("grandchild"); grandchild.setParentId("child");
        var user = new com.ai.system.domain.entity.Menu(); user.setId("accounts"); user.setPath("/system/users");
        assertThat(TenantMenuVisibility.hiddenIds(List.of(grandchild, user, child, tenant)))
                .containsExactlyInAnyOrder("tenant", "child", "grandchild");
    }

    @Test
    void removesTenantNodeAndDescendantsWhileKeepingOtherSystemMenus() {
        MenuVO system = menu("system", null, null, null, null);
        MenuVO tenant = menu("tenant", "system", "system:tenant:list", "/system/tenant", "admin/TenantManagementPage");
        MenuVO tenantChild = menu("tenant-child", "tenant", "system:tenant:add", "/system/tenant/new", null);
        MenuVO user = menu("user", "system", "system:user:list", "/system/user", "admin/UserManagementPage");

        assertThat(TenantMenuVisibility.filterTree(List.of(system, tenant, tenantChild, user)))
                .extracting(MenuVO::getId)
                .containsExactly("system", "user");
    }

    private static MenuVO menu(String id, String parent, String permission, String path, String component) {
        MenuVO menu = new MenuVO();
        menu.setId(id);
        menu.setParentId(parent);
        menu.setPermission(permission);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setMenuType(MenuType.MENU);
        menu.setEnabled(true);
        menu.setVisible(true);
        return menu;
    }
}
