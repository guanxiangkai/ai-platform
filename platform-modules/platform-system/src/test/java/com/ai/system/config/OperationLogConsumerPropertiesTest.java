package com.ai.system.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OperationLogConsumerPropertiesTest {

    @Test
    void recoveryPolicyShouldHaveSafeDefaultsAndValidation() {
        OperationLogConsumerProperties properties = new OperationLogConsumerProperties();
        assertThat(properties.getPendingBatchSize()).isEqualTo(100);
        assertThat(properties.getPollTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.getPendingRecoveryDelay()).isEqualTo(Duration.ofSeconds(30));

        properties.setPendingBatchSize(1_001);
        properties.setPollTimeout(Duration.ZERO);
        properties.setPendingRecoveryDelay(Duration.ofHours(1).plusSeconds(1));
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder(
                            "pendingBatchSize",
                            "pollTimeoutWithinBounds",
                            "pendingRecoveryDelayWithinBounds");
        }
    }
}
