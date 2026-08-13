package com.ai.api.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformSuperAdminTest {

    @Test
    void identifiersShouldRemainStableAcrossServices() {
        assertThat(PlatformSuperAdmin.USER_ID).isEqualTo("platform-super-admin");
        assertThat(PlatformSuperAdmin.TOKEN_VERSION_CACHE_KEY)
                .isEqualTo("platform:auth:super-admin:token-version");
    }
}
