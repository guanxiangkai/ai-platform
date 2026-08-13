package com.ai.api.context;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantExecutionScopeTest {

    @Test
    void runRestoresOuterScope() {
        AtomicReference<String> innerTenant = new AtomicReference<>();
        AtomicReference<String> restoredTenant = new AtomicReference<>();

        TenantExecutionScope.run("tenant-a", () -> {
            TenantExecutionScope.run("tenant-b",
                    () -> innerTenant.set(TenantExecutionScope.currentTenantId()));
            restoredTenant.set(TenantExecutionScope.currentTenantId());
        });

        assertThat(innerTenant).hasValue("tenant-b");
        assertThat(restoredTenant).hasValue("tenant-a");
        assertThat(TenantExecutionScope.currentTenantId()).isNull();
    }

    @Test
    void runRejectsBlankTenant() {
        assertThatThrownBy(() -> TenantExecutionScope.run("  ", () -> { }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tenantId must not be blank");
    }
}
