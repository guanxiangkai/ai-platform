package com.ai.system.controller;

import com.ai.api.security.PlatformSuperAdmin;
import com.ai.system.domain.dto.TenantDTO;
import com.ai.system.domain.dto.TenantPageDTO;
import com.ai.system.domain.vo.TenantPageVO;
import com.ai.system.domain.vo.TenantVO;
import com.ai.system.service.ITenantService;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class TenantControllerGuardTest {

    private final ITenantService service = mock(ITenantService.class);

    @AfterEach
    void clearUser() {
        UserContextHolder.clear();
    }

    @Test
    void inheritedCrudAndCustomTenantEndpointsRejectOrdinaryWildcardUser() {
        UserContextHolder.set(user("tenant-admin", false));
        TenantController controller = proxy();
        assertThatThrownBy(() -> controller.options()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> controller.checkTenantCode("tenant")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> controller.create(new TenantDTO(null, null, null, "t", "t", null, null, null, null, null, null, null, null, null, null))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> controller.list(new TenantPageDTO()).block()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void everyInheritedTenantResourceOperationIsGuardedBeforeServiceInvocation() {
        UserContextHolder.set(user("tenant-admin", false));
        TenantController controller = proxy();
        Set<String> operations = Set.of("list", "detail", "create", "update", "delete", "batchDelete",
                "updateEnabled", "batchUpdateEnabled", "importData", "options", "checkTenantCode");
        for (var method : TenantController.class.getMethods()) {
            if (operations.contains(method.getName())) {
                assertThatThrownBy(() -> method.invoke(controller, new Object[method.getParameterCount()]))
                        .hasRootCauseInstanceOf(io.github.guanxiangkai.web.plus.error.exception.PermissionDeniedException.class);
            }
        }
        verifyNoInteractions(service);
    }

    @Test
    void fixedPlatformSuperAdminCanUseInheritedAndCustomEndpoints() {
        when(service.create(org.mockito.ArgumentMatchers.any())).thenReturn("tenant-id");
        when(service.options()).thenReturn(java.util.List.of());
        when(service.checkTenantCode("tenant")).thenReturn(true);
        when(service.list(org.mockito.ArgumentMatchers.any())).thenReturn(PageResponse.of(java.util.List.<TenantPageVO>of(), 0L, 1, 20));
        UserContextHolder.set(user(PlatformSuperAdmin.USER_ID, true));
        TenantController controller = proxy();
        controller.options();
        controller.checkTenantCode("tenant");
        controller.create(new TenantDTO(null, null, null, "t", "t", null, null, null, null, null, null, null, null, null, null)).block();
        controller.list(new TenantPageDTO()).block();
    }

    private TenantController proxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new TenantController(service));
        factory.addAspect(new com.ai.system.security.PlatformSuperAdminGuardAspect());
        return factory.getProxy();
    }

    private static UserContext user(String id, boolean superAdmin) {
        return new UserContext(id, "tenant-1", superAdmin, null, Set.of(), Set.of(), Set.of("*"), Map.of());
    }
}
