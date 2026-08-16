package com.ai.gateway.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AiGatewayPropertiesTest {

    @Test
    void rateLimitBackendFailureIsFailClosedByDefault() {
        assertThat(new AiGatewayProperties().isRateLimitFailOpen()).isFalse();
    }

    @Test
    void normalizesAndDeduplicatesExactTrustedProxyIps() {
        AiGatewayProperties properties = new AiGatewayProperties();

        properties.setTrustedProxyIps(List.of(" 192.0.2.10 ", "192.0.2.10", "2001:db8::1"));

        assertThat(properties.getTrustedProxyIps())
                .containsExactly("192.0.2.10", "2001:db8:0:0:0:0:0:1");
    }

    @Test
    void rejectsHostnamesAndCidrsFromTrustedProxyIps() {
        AiGatewayProperties properties = new AiGatewayProperties();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties.setTrustedProxyIps(List.of("proxy.internal")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties.setTrustedProxyIps(List.of("192.0.2.0/24")));
    }

    @Test
    void rejectsBlankJwtProfileAndExcessiveClockSkew() {
        AiGatewayProperties properties = new AiGatewayProperties();

        assertThatIllegalArgumentException().isThrownBy(() -> properties.setKeyId(" "));
        assertThatIllegalArgumentException().isThrownBy(() -> properties.setIssuer(" "));
        assertThatIllegalArgumentException().isThrownBy(() -> properties.setAccessTokenAudience(" "));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties.setAllowedClockSkew(Duration.ofMinutes(6)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties.setAllowedClockSkew(Duration.ofSeconds(-1)));
    }
}
