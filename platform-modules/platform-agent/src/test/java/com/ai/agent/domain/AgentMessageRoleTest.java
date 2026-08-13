package com.ai.agent.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentMessageRoleTest {

    @Test
    void shouldOnlyAcceptConversationRoles() {
        assertThat(AgentMessageRole.USER.value()).isEqualTo("user");
        assertThat(AgentMessageRole.ASSISTANT.value()).isEqualTo("assistant");
        assertThat(AgentMessageRole.contains("user")).isTrue();
        assertThat(AgentMessageRole.contains("assistant")).isTrue();
        assertThat(AgentMessageRole.contains("system")).isFalse();
    }
}
