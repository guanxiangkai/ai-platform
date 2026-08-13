package com.ai.system.config;

import com.ai.api.context.TenantExecutionScope;
import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextConfigurationTest {

    private final TenantIdProvider provider = new TenantContextConfiguration().platformTenantIdProvider();

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void backgroundScopeTakesPriorityAndThenRestoresRequestTenant() {
        UserContextHolder.set(new UserContext(
                "user-1", "request-tenant", false, null, Set.of(), Set.of(), Set.of(), Map.of()));
        AtomicReference<String> scopedTenant = new AtomicReference<>();

        TenantExecutionScope.run("job-tenant", () -> scopedTenant.set(provider.getCurrentTenantId()));

        assertThat(scopedTenant).hasValue("job-tenant");
        assertThat(provider.getCurrentTenantId()).isEqualTo("request-tenant");
    }
}
