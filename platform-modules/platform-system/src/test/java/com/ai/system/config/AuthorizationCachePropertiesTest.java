package com.ai.system.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationCachePropertiesTest {

    @Test
    void ttlShouldHaveSafeDefaultAndValidation() {
        AuthorizationCacheProperties properties = new AuthorizationCacheProperties();
        assertThat(properties.getTtl()).isEqualTo(Duration.ofMinutes(5));

        properties.setTtl(Duration.ofDays(1).plusSeconds(1));
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(properties))
                    .extracting(violation -> violation.getMessage())
                    .containsExactly("授权范围缓存有效期必须大于 0 且不超过 1 天");
        }
    }
}
