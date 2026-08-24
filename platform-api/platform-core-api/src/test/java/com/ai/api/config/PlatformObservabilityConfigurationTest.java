package com.ai.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformObservabilityConfigurationTest {

    @Test
    void sharedConfigurationEnablesW3cPropagationAndSafeExporterDefaults() throws Exception {
        ClassPathResource resource = new ClassPathResource("platform-observability.yml");
        var propertySource = new YamlPropertySourceLoader().load("platform-observability", resource).getFirst();

        assertThat(propertySource.getProperty("management.tracing.propagation.type")).isEqualTo("W3C");
        assertThat(propertySource.getProperty("management.tracing.export.otlp.enabled"))
                .isEqualTo("${OTEL_TRACING_EXPORT_ENABLED:false}");
        assertThat(propertySource.getProperty("spring.task.execution.propagate-context")).isEqualTo(true);
        assertThat(propertySource.getProperty("logging.pattern.correlation"))
                .isEqualTo("[${spring.application.name:},%X{traceId:-},%X{spanId:-}] ");
    }
}
