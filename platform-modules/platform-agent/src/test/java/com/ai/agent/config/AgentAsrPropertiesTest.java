package com.ai.agent.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAsrPropertiesTest {

    @Test
    void shouldBindDurationAndDataSize() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "platform.agent.speech-to-text-timeout", "60s",
                "platform.agent.speech-to-text-max-upload-size", "10MB"
        ));

        AgentAsrProperties properties = new Binder(source)
                .bind("platform.agent", Bindable.of(AgentAsrProperties.class))
                .orElseThrow(() -> new AssertionError("ASR 属性绑定结果不能为空"));

        assertThat(properties.getSpeechToTextTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.getSpeechToTextMaxUploadSize()).isEqualTo(DataSize.ofMegabytes(10));
    }

    @Test
    void shouldRejectUnsafeRuntimeLimits() {
        AgentAsrProperties properties = new AgentAsrProperties();
        properties.setSpeechToTextTimeout(Duration.ZERO);
        properties.setSpeechToTextMaxUploadSize(DataSize.ofGigabytes(2));

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder(
                            "speechToTextTimeoutWithinBounds",
                            "speechToTextMaxUploadSizeWithinBounds");
        }
    }
}
