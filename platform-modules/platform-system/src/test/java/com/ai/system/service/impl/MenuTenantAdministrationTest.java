package com.ai.system.service.impl;

import com.ai.system.domain.dto.MenuPageDTO;
import com.ai.system.domain.entity.Menu;
import com.ai.system.repository.MenuRepository;
import com.ai.system.repository.RoleMenuRepository;
import com.ai.system.repository.UserRoleRepository;
import com.ai.system.security.AuthorizationCacheService;
import io.github.guanxiangkai.web.plus.error.exception.PermissionDeniedException;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MenuTenantAdministrationTest {
    private final MenuRepository repository = mock(MenuRepository.class);
    private final MenuServiceImpl service = new MenuServiceImpl(repository, mock(UserRoleRepository.class),
            mock(RoleMenuRepository.class), mock(AuthorizationCacheService.class), mock(ObjectProvider.class));

    private void menus() {
        var tenant = new Menu(); tenant.setId("tenant"); tenant.setPath("/system/tenants");
        var child = new Menu(); child.setId("child"); child.setParentId("tenant");
        var grandchild = new Menu(); grandchild.setId("grandchild"); grandchild.setParentId("child");
        when(repository.findByDeletedFalse()).thenReturn(List.of(grandchild, child, tenant));
        when(repository.findByIdAndDeletedFalse("grandchild")).thenReturn(Optional.of(grandchild));
    }

    @Test
    @SuppressWarnings("unchecked")
    void paginationFiltersAllTenantDescendantsInTheDatabasePredicate() {
        menus();
        Root<Menu> root = mock(Root.class);
        Path<Object> id = mock(Path.class);
        when(root.get("id")).thenReturn(id);
        service.buildQuerySpec(new MenuPageDTO()).toPredicate(root, mock(CriteriaQuery.class), mock(CriteriaBuilder.class));
        verify(id).in(Set.of("tenant", "child", "grandchild"));
    }

    @Test
    void existingPlainDescendantCannotBeDeletedOrEnabled() {
        menus();
        assertThatThrownBy(() -> service.delete("grandchild")).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> service.updateEnabled("grandchild", false)).isInstanceOf(PermissionDeniedException.class);
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any(Menu.class));
    }
}
