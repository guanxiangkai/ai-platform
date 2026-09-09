package com.ai.system.security;

import com.ai.api.security.PlatformSuperAdmin;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import com.ai.system.controller.TenantController;
import com.ai.system.service.ITenantService;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformSuperAdminGuardAspectTest {

    private final Fixture target = new Fixture();

    @AfterEach
    void clearUser() {
        UserContextHolder.clear();
    }

    @Test
    void deniesAnonymousOrdinaryAndForgedSuperAdminClaims() {
        Fixture proxy = proxy();
        assertDenied(proxy);
        UserContextHolder.set(user("tenant-admin", false, "*"));
        assertDenied(proxy);
        UserContextHolder.set(user("forged", true, "*"));
        assertDenied(proxy);
    }

    @Test
    void allowsOnlyFixedPlatformSuperAdminIdentity() {
        UserContextHolder.set(user(PlatformSuperAdmin.USER_ID, true, "*"));
        assertThat(proxy().options().data()).isEmpty();
    }

    private Fixture proxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new PlatformSuperAdminGuardAspect());
        return factory.getProxy();
    }

    private void assertDenied(Fixture proxy) {
        assertThatThrownBy(proxy::options).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("平台超级管理员");
    }

    private static UserContext user(String id, boolean superAdmin, String... permissions) {
        return new UserContext(id, "tenant-1", superAdmin, null, java.util.Set.of(), java.util.Set.of(),
                java.util.Set.of(permissions), java.util.Map.of());
    }

    static class Fixture extends TenantController {
        Fixture() { super(mock(ITenantService.class)); }
    }
}
