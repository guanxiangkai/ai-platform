package com.ai.agent.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** 用户端会话的身份、变量与连续对话边界测试。 */
class AgentSessionSecurityContractTest {
    @Test
    void sessionMustUseOwnerIdentityTrustedVariablesAndBoundedHistory() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/com/ai/agent/service/AgentSessionService.java"));
        String invocation = Files.readString(Path.of(
                "src/main/java/com/ai/agent/service/AgentInvocationTransactionService.java"));
        String dify = Files.readString(Path.of(
                "src/main/java/com/ai/agent/integration/DifyAgentClient.java"));

        assertThat(session).contains("AgentInvocationProperties", "AgentTrustedVariable",
                "getMaxVariableDepth", "getMaxVariableTotalSize", "getDeptId", "getDeptIds")
                .doesNotContain("TRUSTED_VARIABLES", "MAX_VARIABLE_DEPTH", "MAX_VARIABLE_TOTAL_SIZE");
        assertThat(invocation).contains("!Objects.equals(userId, session.getUserId())",
                "getMaxHistoryMessages", "getMaxHistoryCharacters", "getProviderConversationId")
                .doesNotContain("MAX_HISTORY_MESSAGES", "MAX_HISTORY_CHARACTERS")
                .doesNotContain("!SecurityUtils.isSuperAdmin() && !userId.equals(session.getUserId())");
        assertThat(dify).contains("conversation_id", "providerConversationId");
    }
}
