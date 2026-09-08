package com.ai.sse.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuperAdminSseVisibilityTest {

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void trustedPlatformSuperAdminCanViewSuperAdminTargets() {
        setContext(PlatformSuperAdmin.USER_ID, true);

        assertThat(SuperAdminSseVisibility.canViewSuperAdminRecords()).isTrue();
        assertThat(SuperAdminSseVisibility.isVisible(
                PlatformSuperAdmin.USER_ID, "user-1," + PlatformSuperAdmin.USER_ID, null, null))
                .isTrue();
    }

    @Test
    void ordinaryUserCannotViewPushContainingSuperAdminAmongManyTargets() {
        setContext("user-1", false);

        assertThat(SuperAdminSseVisibility.isVisible(
                "user-2", "user-2," + PlatformSuperAdmin.USER_ID, null, null))
                .isFalse();
        assertThat(SuperAdminSseVisibility.isVisible(
                "user-2", PlatformSuperAdmin.USER_ID + ",user-2", null, null)).isFalse();
        assertThat(SuperAdminSseVisibility.isVisible(
                "user-2", "user-2," + PlatformSuperAdmin.USER_ID + ",user-3", null, null)).isFalse();
        assertThat(SuperAdminSseVisibility.isVisible(
                "user-2", PlatformSuperAdmin.USER_ID, null, null)).isFalse();

        assertThat(SuperAdminSseVisibility.isVisible(
                "user-2", "platform-super-admin-extra", null, null)).isTrue();
    }

    @Test
    void spoofedSuperFlagAndUnauthenticatedUserCannotViewSuperAdminDetail() {
        setContext("user-1", true);
        assertThatThrownBy(() -> SuperAdminSseVisibility.requireVisible(
                PlatformSuperAdmin.USER_ID, null, null, "SSE 连接记录", "record-1"))
                .isInstanceOf(RuntimeException.class);

        UserContextHolder.clear();
        assertThatThrownBy(() -> SuperAdminSseVisibility.requireVisible(
                PlatformSuperAdmin.USER_ID, null, null, "SSE 连接记录", "record-1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void auditSubjectFieldsAlsoHideRecord() {
        setContext("user-1", false);

        assertThat(SuperAdminSseVisibility.isVisible(
                "user-2", null, PlatformSuperAdmin.USER_ID, null)).isFalse();
        assertThat(SuperAdminSseVisibility.isVisible(
                "user-2", null, null, PlatformSuperAdmin.USER_ID)).isFalse();
    }

    private void setContext(String userId, boolean superAdmin) {
        UserContextHolder.set(new UserContext(
                userId, "tenant-1", superAdmin, null,
                Set.of(), Set.of(), Set.of(), Map.of()));
    }
}
