package com.ai.system.service.impl;

import com.ai.system.domain.MenuType;
import com.ai.system.domain.vo.MenuVO;
import com.ai.system.repository.MenuRepository;
import com.ai.system.repository.RoleMenuRepository;
import com.ai.system.repository.UserRoleRepository;
import com.ai.system.security.AuthorizationCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MenuServiceNavigationPolicyTest {

    @Test
    void authorizedPageShouldIncludeItsVisibleParentDirectories() {
        MenuServiceImpl service = service();
        MenuVO root = menu("root", null, MenuType.DIRECTORY, true, true);
        MenuVO section = menu("section", "root", MenuType.DIRECTORY, true, true);
        MenuVO page = menu("page", "section", MenuType.MENU, true, true);

        List<MenuVO> result = service.includeAuthorizedNavigationMenus(
                List.of(root, section, page), List.of("page"));

        assertThat(result).extracting(MenuVO::getId).containsExactly("root", "section", "page");
    }

    @Test
    void disabledParentShouldCloseItsAuthorizedChildRoute() {
        MenuServiceImpl service = service();
        MenuVO root = menu("root", null, MenuType.DIRECTORY, true, true);
        MenuVO section = menu("section", "root", MenuType.DIRECTORY, false, true);
        MenuVO page = menu("page", "section", MenuType.MENU, true, true);

        List<MenuVO> result = service.includeAuthorizedNavigationMenus(
                List.of(root, section, page), List.of("page"));

        assertThat(result).isEmpty();
    }

    @Test
    void buttonAuthorizationShouldNotCreateNavigationRoute() {
        MenuServiceImpl service = service();
        MenuVO button = menu("button", null, MenuType.BUTTON, true, true);

        assertThat(service.includeAuthorizedNavigationMenus(List.of(button), List.of("button"))).isEmpty();
    }

    private MenuServiceImpl service() {
        return new MenuServiceImpl(
                mock(MenuRepository.class),
                mock(UserRoleRepository.class),
                mock(RoleMenuRepository.class),
                mock(AuthorizationCacheService.class),
                mock(ObjectProvider.class)
        );
    }

    private MenuVO menu(String id, String parentId, MenuType type, boolean enabled, boolean visible) {
        MenuVO menu = new MenuVO();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuType(type);
        menu.setEnabled(enabled);
        menu.setVisible(visible);
        return menu;
    }
}
