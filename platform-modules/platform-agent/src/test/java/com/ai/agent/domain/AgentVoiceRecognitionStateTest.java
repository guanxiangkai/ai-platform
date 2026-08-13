package com.ai.agent.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentVoiceRecognitionStateTest {

    @Test
    void persistedNamesShouldMatchCurrentDatabaseContract() {
        assertThat(AgentVoiceRecognitionState.values())
                .extracting(AgentVoiceRecognitionState::name)
                .containsExactly("RUNNING", "SUCCEEDED", "NEED_REVIEW", "FAILED");
    }
}
