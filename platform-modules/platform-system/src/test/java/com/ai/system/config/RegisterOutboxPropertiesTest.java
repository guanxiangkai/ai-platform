package com.ai.system.config;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

class RegisterOutboxPropertiesTest {
    @Test
    void defaultsShouldBePositiveAndBounded() {
        RegisterOutboxProperties properties = new RegisterOutboxProperties();
        assertThat(properties.hasValidDurations()).isTrue();
        properties.setProcessingLease(Duration.ZERO);
        assertThat(properties.hasValidDurations()).isFalse();
    }
}
