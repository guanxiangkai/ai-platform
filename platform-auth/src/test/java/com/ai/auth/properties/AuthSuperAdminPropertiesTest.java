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
    void enabledConfigurationRequiresUsernameAndDigest() {
        assertThatThrownBy(() -> new AuthSuperAdminProperties(true, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("独立账号和密码摘要");
    }

    @Test
    void enabledConfigurationRejectsPlaintextAndMalformedDigest() {
        assertThatThrownBy(() -> new AuthSuperAdminProperties(true, "admin", "plaintext"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("40位小写SHA-1摘要");
    }

    @Test
    void enabledConfigurationRejectsEmptyDigest() {
        assertThatThrownBy(() -> new AuthSuperAdminProperties(true, "admin", "da39a3ee5e6b4b0d3255bfef95601890afd80709"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("40位小写SHA-1摘要");
    }

    @Test
    void enabledConfigurationRejectsUppercaseDigestAndBcrypt() {
        assertThatThrownBy(() -> new AuthSuperAdminProperties(true, "admin", "A".repeat(40)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthSuperAdminProperties(true, "admin", "$2b$12$" + "A".repeat(53)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enabledConfigurationNormalizesValues() {
        AuthSuperAdminProperties properties = new AuthSuperAdminProperties(
                true,
                " platform-admin ",
                " 0123456789abcdef0123456789abcdef01234567 "
        );

        assertThat(properties.configured()).isTrue();
        assertThat(properties.matchesUsername(" platform-admin ")).isTrue();
        assertThat(properties.username()).isEqualTo("platform-admin");
        assertThat(properties.passwordDigest()).isEqualTo("0123456789abcdef0123456789abcdef01234567");
    }

    @Test
    void toStringMustRedactSuperAdminIdentityAndDigest() {
        AuthSuperAdminProperties properties = new AuthSuperAdminProperties(
                true, "platform-admin", "0123456789abcdef0123456789abcdef01234567");

        assertThat(properties.toString())
                .contains("enabled=true")
                .contains("username=[REDACTED]")
                .contains("passwordDigest=[REDACTED]")
                .doesNotContain("platform-admin")
                .doesNotContain("0123456789abcdef0123456789abcdef01234567");
    }
}
