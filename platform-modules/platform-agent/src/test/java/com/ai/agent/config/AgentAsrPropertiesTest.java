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
        assertThat(properties.getSpeechToTextProtocol()).isEqualTo(AsrUpstreamProtocol.OPENAI_MULTIPART);
        assertThat(properties.getSpeechToTextModel()).isEqualTo("qwen-audio-3.0-asr-flash");
    }

    @Test
    void shouldBindDashScopeProtocolAndModel() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "platform.agent.speech-to-text-protocol", "DASHSCOPE_MULTIMODAL",
                "platform.agent.speech-to-text-model", "qwen-audio-3.0-asr-flash",
                "platform.agent.speech-to-text-url", "https://example.invalid/asr",
                "platform.agent.speech-to-text-api-key", "test-api-key"
        ));

        AgentAsrProperties properties = new Binder(source)
                .bind("platform.agent", Bindable.of(AgentAsrProperties.class))
                .orElseThrow(() -> new AssertionError("ASR 属性绑定结果不能为空"));

        assertThat(properties.getSpeechToTextProtocol()).isEqualTo(AsrUpstreamProtocol.DASHSCOPE_MULTIMODAL);
        assertThat(properties.getSpeechToTextModel()).isEqualTo("qwen-audio-3.0-asr-flash");
        assertThat(properties.isDashScopeConfigurationComplete()).isTrue();
    }

    @Test
    void shouldRejectIncompleteDashScopeConfiguration() {
        AgentAsrProperties properties = new AgentAsrProperties();
        properties.setSpeechToTextProtocol(AsrUpstreamProtocol.DASHSCOPE_MULTIMODAL);
        properties.setSpeechToTextUrl("https://example.invalid/asr");

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("dashScopeConfigurationComplete");
        }
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
