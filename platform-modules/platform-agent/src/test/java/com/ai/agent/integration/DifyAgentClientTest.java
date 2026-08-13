package com.ai.agent.integration;

import com.ai.agent.domain.AgentInvocationMode;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Dify Chat 流式协议回归测试。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
class DifyAgentClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void chatShouldUseStreamingAndWorkflowShouldUseBlocking() {
        assertThat(DifyAgentClient.responseMode(AgentInvocationMode.CHAT)).isEqualTo("streaming");
        assertThat(DifyAgentClient.responseMode(AgentInvocationMode.WORKFLOW)).isEqualTo("blocking");
    }

    @Test
    void shouldAggregateAnswerWithoutExposingThoughtEvents() {
        DifyChatStreamAccumulator accumulator = new DifyChatStreamAccumulator(objectMapper)
                .accept("{\"event\":\"agent_thought\",\"thought\":\"内部推理\"}")
                .accept("{\"event\":\"agent_message\",\"answer\":\"你好\","
                        + "\"conversation_id\":\"conversation-1\"}")
                .accept("{\"event\":\"agent_message\",\"answer\":\"，世界\"}")
                .accept("{\"event\":\"message_end\",\"metadata\":{\"usage\":{"
                        + "\"prompt_tokens\":12,\"completion_tokens\":8}}}");

        AgentProviderResult result = accumulator.result();
        assertThat(result.text()).isEqualTo("你好，世界");
        assertThat(result.providerConversationId()).isEqualTo("conversation-1");
        assertThat(result.inputTokens()).isEqualTo(12);
        assertThat(result.outputTokens()).isEqualTo(8);
    }

    @Test
    void replacementEventShouldReplaceAccumulatedAnswer() {
        AgentProviderResult result = new DifyChatStreamAccumulator(objectMapper)
                .accept("{\"event\":\"message\",\"answer\":\"原始回答\"}")
                .accept("{\"event\":\"message_replace\",\"answer\":\"安全替换回答\"}")
                .result();

        assertThat(result.text()).isEqualTo("安全替换回答");
    }

    @Test
    void errorEventShouldFailWithProviderMessage() {
        DifyChatStreamAccumulator accumulator = new DifyChatStreamAccumulator(objectMapper);

        assertThatThrownBy(() -> accumulator.accept(
                "{\"event\":\"error\",\"code\":\"invalid_param\",\"message\":\"参数无效\"}"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("invalid_param")
                .hasMessageContaining("参数无效");
    }
}
