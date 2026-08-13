package com.ai.agent.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTrustedVariableTest {

    @Test
    void trustedKeysShouldBeUniqueAndClosed() {
        assertThat(Arrays.stream(AgentTrustedVariable.values()).map(AgentTrustedVariable::key))
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrder(
                        "tenantId", "userId", "deptId", "deptIds", "skillId", "scopeTag",
                        "voiceInputActive", "responseMode");
        assertThat(AgentTrustedVariable.contains("tenantId")).isTrue();
        assertThat(AgentTrustedVariable.contains("customVariable")).isFalse();
    }
}
