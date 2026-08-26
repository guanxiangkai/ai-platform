package com.ai.agent.integration;

import com.ai.agent.domain.AgentInvocationMode;
import com.ai.agent.domain.dto.AgentInvocationRequest;
import com.ai.agent.domain.entity.AgentConfig;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

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
    void shouldIgnoreSseEventsWithoutDataBeforeMapping() {
        String answerEvent = "{\"event\":\"agent_message\",\"answer\":\"正常回答\"}";

        List<String> events = DifyAgentClient.chatEventData(Flux.just(
                        ServerSentEvent.<String>builder().event("ping").build(),
                        ServerSentEvent.builder(answerEvent).event("agent_message").build()))
                .collectList()
                .block();

        assertThat(events).containsExactly(answerEvent);
    }

    @Test
    void shouldIgnoreKeepAliveSseFrameWhenDecodedByWebClient() {
        String sseBody = "event: ping\n\n"
                + "event: agent_message\n"
                + "data: {\"event\":\"agent_message\",\"answer\":\"正常回答\"}\n\n"
                + "event: message_end\n"
                + "data: {\"event\":\"message_end\"}\n\n";
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE)
                        .body(sseBody)
                        .build()));
        List<ServerSentEvent<String>> decodedEvents = builder.clone().build().get()
                .uri("https://dify.example/v1/chat-messages")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() { })
                .collectList()
                .block();
        assertThat(decodedEvents).anySatisfy(event -> {
            assertThat(event.event()).isEqualTo("ping");
            assertThat(event.data()).isNull();
        });

        AgentConfig definition = new AgentConfig();
        definition.setInvocationMode(AgentInvocationMode.CHAT);
        definition.setEndpointUrl("https://dify.example/v1/chat-messages");
        definition.setCredential("test-credential");
        AgentInvocationRequest request = new AgentInvocationRequest(
                "invocation-1", null, "你好", null, null, null, java.util.Map.of());

        AgentProviderResult result = new DifyAgentClient(builder, objectMapper).invoke(definition,
                new AgentProviderInvocation(request, "user-1", "invocation-1", null, List.of()));

        assertThat(result.text()).isEqualTo("正常回答");
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
