package com.ai.auth.domain;

import com.ai.auth.domain.vo.LoginResponse;
import com.ai.auth.properties.AuthSuperAdminProperties;
import com.ai.auth.properties.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthValueRedactionTest {

    @Test
    void shouldRedactAuthSnapshotAndConfigurationSecrets() {
        AuthUserSnapshot snapshot = new AuthUserSnapshot(
                "user-secret",
                "username-secret",
                "password-hash-secret",
                true,
                3L,
                "nickname-secret",
                null,
                "USER",
                false,
                "tenant-secret",
                "department-secret",
                Set.of("role-secret"),
                Set.of(),
                Set.of("permission-secret"),
                Set.of()
        );
        AuthSuperAdminProperties superAdmin = new AuthSuperAdminProperties(
                true,
                "administrator-secret",
                "$2b$12$" + "A".repeat(53)
        );
        JwtProperties jwt = new JwtProperties(
                "private-key-secret",
                "key-id",
                Duration.ofHours(2),
                Duration.ofDays(7),
                "issuer",
                "gateway-audience-secret",
                "auth-audience-secret",
                Duration.ofSeconds(60)
        );

        assertThat(snapshot.toString())
                .contains("passwordHash=<redacted>")
                .doesNotContain(
                        "user-secret", "username-secret", "password-hash-secret", "tenant-secret",
                        "department-secret", "role-secret", "permission-secret");
        assertThat(superAdmin.toString())
                .contains("credentials=<redacted>")
                .doesNotContain("administrator-secret", "$2b$12$");
        assertThat(jwt.toString())
                .contains("privateKey=<redacted>")
                .doesNotContain("private-key-secret");
    }

    @Test
    void shouldRedactLoginResponseTokensAndIdentity() {
        LoginResponse response = new LoginResponse(
                "access-token-secret",
                "refresh-token-secret",
                "Bearer",
                7_200L,
                "user-secret",
                "username-secret",
                "name-secret",
                null,
                "USER",
                false,
                Set.of("role-secret"),
                Set.of(),
                Set.of("permission-secret"),
                "department-secret",
                Set.of()
        );

        assertThat(response.toString())
                .contains("tokens=<redacted>", "identity=<redacted>")
                .doesNotContain(
                        "access-token-secret", "refresh-token-secret", "user-secret", "username-secret",
                        "name-secret", "role-secret", "permission-secret", "department-secret");
    }
}
