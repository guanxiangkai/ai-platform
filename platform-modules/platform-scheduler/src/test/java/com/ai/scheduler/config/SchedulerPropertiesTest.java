package com.ai.scheduler.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerPropertiesTest {

    @Test
    void operationalLimitsShouldHaveSafeDefaultsAndValidation() {
        SchedulerProperties properties = new SchedulerProperties();
        assertThat(properties.getMaxPageSize()).isEqualTo(200);
        assertThat(properties.getMaxInstanceTimeLimitMs()).isEqualTo(604_800_000L);
        assertThat(properties.getMaxInstanceCount()).isEqualTo(1_000);
        assertThat(properties.getMaxConcurrency()).isEqualTo(1_000);
        assertThat(properties.getMaxRetryCount()).isEqualTo(100);
        assertThat(properties.getMinFixedIntervalMs()).isEqualTo(1_000);

        properties.setMaxPageSize(0);
        properties.setMaxInstanceTimeLimitMs(999);
        properties.setMaxInstanceCount(0);
        properties.setMaxConcurrency(10_001);
        properties.setMaxRetryCount(10_001);
        properties.setMinFixedIntervalMs(999);
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder(
                            "maxPageSize",
                            "maxInstanceTimeLimitMs",
                            "maxInstanceCount",
                            "maxConcurrency",
                            "maxRetryCount",
                            "minFixedIntervalMs");
        }
    }
}
