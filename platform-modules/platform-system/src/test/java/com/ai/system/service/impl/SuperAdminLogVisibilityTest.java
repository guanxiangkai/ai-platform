package com.ai.system.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import com.ai.system.domain.entity.LoginLog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuperAdminLogVisibilityTest {

    @Test
    void superAdminLogDetailIsNeverExposed() {
        LoginLog log = new LoginLog();
        log.setUserId(PlatformSuperAdmin.USER_ID);

        assertThatThrownBy(() -> SuperAdminLogVisibility.requireVisible(
                log, "LoginLog", "log-1"))
                .isInstanceOf(RuntimeException.class);
    }
}
