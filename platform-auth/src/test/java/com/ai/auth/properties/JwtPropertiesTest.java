package com.ai.auth.properties;

import com.ai.api.security.PlatformTokenProfile;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JwtPropertiesTest {

    @Test
    void appliesStableTokenProfileDefaultsAndNormalizesText() {
        JwtProperties properties = new JwtProperties(
                "private-key",
                "  signing-key  ",
                null,
                null,
                "  ai-platform  ",
                null,
                null,
                null
        );

        assertThat(properties.keyId()).isEqualTo("signing-key");
        assertThat(properties.issuer()).isEqualTo(PlatformTokenProfile.ISSUER);
        assertThat(properties.accessTokenExpiration()).isEqualTo(Duration.ofHours(2));
        assertThat(properties.refreshTokenExpiration()).isEqualTo(Duration.ofDays(7));
        assertThat(properties.accessTokenAudience()).isEqualTo(PlatformTokenProfile.ACCESS_TOKEN_AUDIENCE);
        assertThat(properties.refreshTokenAudience()).isEqualTo(PlatformTokenProfile.REFRESH_TOKEN_AUDIENCE);
        assertThat(properties.allowedClockSkew()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void rejectsNonPositiveLifetimesAndClockSkewOutsideBoundary() {
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ZERO, Duration.ofDays(7), Duration.ofSeconds(60)));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofHours(2), Duration.ofSeconds(-1), Duration.ofSeconds(60)));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofHours(2), Duration.ofDays(7), Duration.ofMinutes(6)));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofHours(2), Duration.ofDays(7), Duration.ofSeconds(-1)));
    }

    private static JwtProperties properties(Duration accessExpiration,
                                            Duration refreshExpiration,
                                            Duration allowedClockSkew) {
        return new JwtProperties(
                "private-key",
                "signing-key",
                accessExpiration,
                refreshExpiration,
                "ai-platform",
                PlatformTokenProfile.ACCESS_TOKEN_AUDIENCE,
                PlatformTokenProfile.REFRESH_TOKEN_AUDIENCE,
                allowedClockSkew
        );
    }
}
