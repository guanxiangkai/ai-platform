package com.ai.auth.domain.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRequestRedactionTest {

    @Test
    void shouldRedactLoginSecretsFromStringRepresentation() {
        LoginRequest request = new LoginRequest(
                "example-user", "password-secret", "captcha-secret", "captcha-key-secret");

        assertThat(request.toString())
                .contains("<redacted>")
                .doesNotContain(
                        "example-user", "password-secret", "captcha-secret", "captcha-key-secret");
    }

    @Test
    void shouldRedactRefreshTokenFromStringRepresentation() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token-secret");

        assertThat(request.toString())
                .contains("<redacted>")
                .doesNotContain("refresh-token-secret");
    }
}
