package com.ai.agent.domain.entity;

import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AgentConfigMappingTest {
    @Test
    void shouldUseAiPrefixedGenericAgentTablesWithoutProductFields() {
        assertThat(Map.of(
                AgentConfig.class, "ai_agent_config",
                AgentSessionRecord.class, "ai_agent_session_record",
                AgentMessageRecord.class, "ai_agent_message_record",
                AgentCallRecord.class, "ai_agent_call_record",
                AgentVoiceRecord.class, "ai_agent_voice_record",
                Skill.class, "ai_skill",
                SkillAgentRelation.class, "ai_skill_agent_relation",
                SkillScopeType.class, "ai_skill_scope_type",
                UserSkillPermission.class, "ai_user_skill_permission"))
                .allSatisfy((entityType, tableName) ->
                        assertThat(entityType.getAnnotation(Table.class).name()).isEqualTo(tableName));
        Set<String> fields = Arrays.stream(AgentConfig.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());

        assertThat(fields).contains("providerType", "invocationMode", "runtimeConfig", "publishState");
        assertThat(fields).doesNotContain("businessType", "projectId", "deptId", "news");
    }
}
