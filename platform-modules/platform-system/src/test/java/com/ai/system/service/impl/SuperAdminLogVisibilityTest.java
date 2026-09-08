package com.ai.system.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import com.ai.system.domain.entity.LoginLog;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuperAdminLogVisibilityTest {

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void trustedPlatformSuperAdminCanViewSuperAdminLogDetail() {
        LoginLog log = new LoginLog();
        log.setUserId(PlatformSuperAdmin.USER_ID);

        UserContextHolder.set(new UserContext(
                PlatformSuperAdmin.USER_ID, "tenant-1", true, null,
                Set.of(), Set.of(), Set.of(), Map.of()));

        assertThat(SuperAdminLogVisibility.canViewSuperAdminLogs()).isTrue();
        SuperAdminLogVisibility.requireVisible(
                log, "LoginLog", "log-1");
    }

    @Test
    void superFlagWithUnrelatedUserIdCannotViewSuperAdminLogDetail() {
        LoginLog log = new LoginLog();
        log.setUserId(PlatformSuperAdmin.USER_ID);
        UserContextHolder.set(new UserContext(
                "user-1", "tenant-1", true, null, Set.of(), Set.of(), Set.of(), Map.of()));

        assertThatThrownBy(() -> SuperAdminLogVisibility.requireVisible(
                log, "LoginLog", "log-1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void unauthenticatedUserCannotViewSuperAdminLogDetail() {
        LoginLog log = new LoginLog();
        log.setUserId(PlatformSuperAdmin.USER_ID);

        assertThatThrownBy(() -> SuperAdminLogVisibility.requireVisible(
                log, "LoginLog", "log-1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void ordinaryLogRemainsVisibleToOrdinaryUser() {
        LoginLog log = new LoginLog();
        log.setUserId("user-1");

        assertThat(SuperAdminLogVisibility.canViewSuperAdminLogs()).isFalse();
        SuperAdminLogVisibility.requireVisible(log, "LoginLog", "log-1");
    }
}
