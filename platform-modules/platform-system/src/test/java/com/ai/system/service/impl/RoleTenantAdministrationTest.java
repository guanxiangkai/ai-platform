package com.ai.system.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import com.ai.system.config.SystemQueryProperties;
import com.ai.system.domain.entity.Menu;
import com.ai.system.domain.entity.Role;
import com.ai.system.domain.entity.RoleMenu;
import com.ai.system.repository.MenuRepository;
import com.ai.system.repository.RoleMenuRepository;
import com.ai.system.repository.RoleRepository;
import com.ai.system.repository.UserRoleRepository;
import com.ai.system.security.AuthUserCacheService;
import com.ai.system.security.AuthorizationCacheService;
import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.web.plus.core.context.CurrentUser;
import io.github.guanxiangkai.web.plus.core.context.CurrentUserHolder;
import io.github.guanxiangkai.web.plus.error.exception.PermissionDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class RoleTenantAdministrationTest {

    private final RoleRepository roles = mock(RoleRepository.class);
    private final SystemQueryProperties queryProperties = mock(SystemQueryProperties.class);
    private final UserRoleRepository userRoles = mock(UserRoleRepository.class);
    private final RoleMenuRepository roleMenus = mock(RoleMenuRepository.class);
    private final MenuRepository menus = mock(MenuRepository.class);
    private final AuthorizationCacheService authorization = mock(AuthorizationCacheService.class);
    private final ObjectProvider<AuthUserCacheService> authUsers = mock(ObjectProvider.class);
    private final TenantIdProvider tenants = () -> "tenant-1";
    private final RoleServiceImpl service = new RoleServiceImpl(
            roles, queryProperties, userRoles, roleMenus, menus, authorization, authUsers, tenants);
    private final Role role = role("role-1");
    private final Menu tenantRoot = menu("tenant-root", null, "system:tenant:list", "/system/tenant", "admin/TenantManagementPage");
    private final Menu tenantChild = menu("tenant-child", "tenant-root", "system:custom:list", "/system/generic", null);
    private final Menu ordinary = menu("user-menu", null, "system:user:list", "/system/user", "admin/UserManagementPage");

    @BeforeEach
    void setUp() {
        when(roles.findById("role-1")).thenReturn(Optional.of(role));
        when(roles.existsByRoleCodeAndDeletedFalse(any())).thenReturn(false);
        when(roles.save(any(Role.class))).thenAnswer(invocation -> {
            Role saved = invocation.getArgument(0); if (saved.getId() == null) saved.setId("copied-role"); return saved;
        });
        when(menus.findByDeletedFalse()).thenReturn(List.of(tenantRoot, tenantChild, ordinary));
        when(menus.findByIdAndDeletedFalse("tenant-root")).thenReturn(Optional.of(tenantRoot));
        when(menus.findByIdAndDeletedFalse("tenant-child")).thenReturn(Optional.of(tenantChild));
        when(menus.findByIdAndDeletedFalse("user-menu")).thenReturn(Optional.of(ordinary));
        when(authUsers.getIfAvailable()).thenReturn(null);
        CurrentUserHolder.set(user("tenant-admin", false));
    }

    @AfterEach
    void clearUser() {
        CurrentUserHolder.clear();
    }

    @Test
    void ordinaryUserCannotAddTenantRootOrDescendantAndDoesNotWriteRelations() {
        when(roleMenus.findByRoleId("role-1")).thenReturn(List.of(roleMenu("user-menu")));

        assertThatThrownBy(() -> service.assignPermissions("role-1", List.of("user-menu", "tenant-child")))
                .isInstanceOf(PermissionDeniedException.class);

        verify(roleMenus, never()).deleteByRoleId("role-1");
        verify(roleMenus, never()).saveAll(any());
    }

    @Test
    void ordinaryEditPreservesExistingHiddenBindingsWhileSavingVisibleChanges() {
        when(roleMenus.findByRoleId("role-1")).thenReturn(List.of(
                roleMenu("tenant-root"), roleMenu("tenant-child"), roleMenu("user-menu")));

        assertThat(service.assignPermissions("role-1", List.of("user-menu"))).isTrue();

        var saved = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(roleMenus).saveAll(saved.capture());
        assertThat(((List<RoleMenu>) saved.getValue()).stream().map(RoleMenu::getMenuId))
                .containsExactlyInAnyOrder("tenant-root", "tenant-child", "user-menu");
    }

    @Test
    void ordinaryRolePermissionReadHidesTenantRootAndDescendant() {
        when(roleMenus.findByRoleId("role-1")).thenReturn(List.of(
                roleMenu("tenant-root"), roleMenu("tenant-child"), roleMenu("user-menu")));

        assertThat(service.getRolePermissions("role-1")).containsExactly("user-menu");
    }

    @Test
    void ordinaryDuplicateExcludesHiddenBindings() {
        when(roleMenus.findByRoleId("role-1")).thenReturn(List.of(
                roleMenu("tenant-root"), roleMenu("tenant-child"), roleMenu("user-menu")));

        service.duplicate("role-1");

        var saved = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(roleMenus).saveAll(saved.capture());
        assertThat(((List<RoleMenu>) saved.getValue()).stream().map(RoleMenu::getMenuId))
                .containsExactly("user-menu");
    }

    @Test
    void fixedPlatformSuperAdminDuplicateCopiesHiddenBindings() {
        CurrentUserHolder.set(user(PlatformSuperAdmin.USER_ID, true));
        when(roleMenus.findByRoleId("role-1")).thenReturn(List.of(
                roleMenu("tenant-root"), roleMenu("tenant-child"), roleMenu("user-menu")));

        service.duplicate("role-1");

        var saved = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(roleMenus).saveAll(saved.capture());
        assertThat(((List<RoleMenu>) saved.getValue()).stream().map(RoleMenu::getMenuId))
                .containsExactlyInAnyOrder("tenant-root", "tenant-child", "user-menu");
    }

    private static Role role(String id) {
        Role role = new Role();
        role.setId(id);
        role.setRoleCode("role-code");
        role.setRoleName("测试角色");
        role.setTenantId("tenant-1");
        return role;
    }

    private static RoleMenu roleMenu(String menuId) {
        RoleMenu relation = new RoleMenu();
        relation.setRoleId("role-1");
        relation.setMenuId(menuId);
        relation.setTenantId("tenant-1");
        return relation;
    }

    private static Menu menu(String id, String parentId, String permission, String path, String component) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setPermission(permission);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setTenantId("tenant-1");
        menu.setDeleted(false);
        return menu;
    }

    private static CurrentUser user(String id, boolean superAdmin) {
        return new CurrentUser(id, null, "tenant-1", null, Set.of(), Set.of(), Set.of("*"), superAdmin,
                null, System.currentTimeMillis(), Map.of());
    }
}
