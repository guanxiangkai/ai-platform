package com.ai.agent.service;

import com.ai.agent.domain.dto.AgentInvocationRequest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentInvocationFingerprintTest {

    @Test
    void variableMapOrderShouldNotChangeFingerprint() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("b", List.of(1, 2));
        first.put("a", Map.of("value", true));
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("a", Map.of("value", true));
        second.put("b", List.of(1, 2));

        assertThat(AgentInvocationFingerprint.create("agent-1", request(first)))
                .isEqualTo(AgentInvocationFingerprint.create("agent-1", request(second)))
                .hasSize(64);
    }

    @Test
    void invocationIdShouldNotChangeRequestFingerprint() {
        AgentInvocationRequest first = request(Map.of());
        AgentInvocationRequest second = new AgentInvocationRequest(
                "another-id", first.sessionId(), first.message(), first.sessionTitle(),
                first.contextNamespace(), first.contextReference(), first.variables());

        assertThat(AgentInvocationFingerprint.create("agent-1", first))
                .isEqualTo(AgentInvocationFingerprint.create("agent-1", second));
    }

    private AgentInvocationRequest request(Map<String, Object> variables) {
        return new AgentInvocationRequest(
                "invocation-1", "session-1", "hello", null, "team", "skill-1", variables);
    }
}
