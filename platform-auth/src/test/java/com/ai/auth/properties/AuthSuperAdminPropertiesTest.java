package com.ai.auth.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthSuperAdminPropertiesTest {

    @Test
    void disabledConfigurationMayRemainEmpty() {
        AuthSuperAdminProperties properties = new AuthSuperAdminProperties(false, null, null);

        assertThat(properties.configured()).isFalse();
    }

    @Test
    void enabledConfigurationRequiresUsernameAndHash() {
        assertThatThrownBy(() -> new AuthSuperAdminProperties(true, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("独立账号和密码哈希");
    }

    @Test
    void enabledConfigurationRequiresProtocolBcryptHash() {
        assertThatThrownBy(() -> new AuthSuperAdminProperties(true, "admin", "{bcrypt}encoded"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sha1-bcrypt");
    }

    @Test
    void enabledConfigurationNormalizesValues() {
        AuthSuperAdminProperties properties = new AuthSuperAdminProperties(
                true,
                " platform-admin ",
                " {sha1-bcrypt}$2b$12$" + "A".repeat(53) + " "
        );

        assertThat(properties.configured()).isTrue();
        assertThat(properties.matchesUsername(" platform-admin ")).isTrue();
        assertThat(properties.username()).isEqualTo("platform-admin");
        assertThat(properties.passwordHash()).startsWith("{sha1-bcrypt}$2b$12$");
    }
}
