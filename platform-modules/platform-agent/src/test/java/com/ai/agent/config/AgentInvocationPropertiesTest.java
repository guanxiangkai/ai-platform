package com.ai.agent.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AgentInvocationPropertiesTest {

    @Test
    void shouldBindOperationalLimits() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "platform.agent.invocation.max-history-messages", 12,
                "platform.agent.invocation.execution-lease", "20m",
                "platform.agent.invocation.max-variable-depth", 6,
                "platform.agent.invocation.max-variable-total-size", 32_000
        ));

        AgentInvocationProperties properties = new Binder(source)
                .bind("platform.agent.invocation", Bindable.of(AgentInvocationProperties.class))
                .orElseThrow(() -> new AssertionError("智能体调用属性绑定结果不能为空"));

        assertThat(properties.getMaxHistoryMessages()).isEqualTo(12);
        assertThat(properties.getExecutionLease()).isEqualTo(Duration.ofMinutes(20));
        assertThat(properties.getMaxVariableDepth()).isEqualTo(6);
        assertThat(properties.getMaxVariableTotalSize()).isEqualTo(32_000);
        assertThat(properties.getMaxVariableCount()).isEqualTo(32);
    }

    @Test
    void shouldRejectUnsafeLimits() {
        AgentInvocationProperties properties = new AgentInvocationProperties();
        properties.setMaxHistoryMessages(0);
        properties.setExecutionLease(Duration.ofMinutes(11));
        properties.setMaxVariableDepth(17);
        properties.setMaxVariableTotalSize(4_000_001);

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder(
                            "maxHistoryMessages", "executionLeaseWithinBounds",
                            "maxVariableDepth", "maxVariableTotalSize");
        }
    }
}
