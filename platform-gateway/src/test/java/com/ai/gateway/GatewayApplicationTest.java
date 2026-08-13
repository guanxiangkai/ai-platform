package com.ai.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayApplicationTest {

    @Test
    void usesGatewayNativeSecurityConfiguration() {
        SpringBootApplication annotation = GatewayApplication.class.getAnnotation(SpringBootApplication.class);

        assertThat(annotation.excludeName())
                .contains("org.springframework.cloud.gateway.config.GatewayRedisAutoConfiguration")
                .doesNotContain("io.github.guanxiangkai.web.plus.security.config.WebPlusAuthAutoConfiguration");
    }
}
