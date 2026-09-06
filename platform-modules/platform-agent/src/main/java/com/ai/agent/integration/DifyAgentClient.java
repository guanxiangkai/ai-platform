package com.ai.agent.integration;

import com.ai.agent.domain.AgentInvocationMode;
import com.ai.agent.domain.AgentProviderType;
import com.ai.agent.domain.entity.AgentConfig;
import com.ai.api.agent.AgentApiHeaders;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dify 对话与工作流应用协议适配器。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class DifyAgentClient implements AgentProviderClient {
    private static final ParameterizedTypeReference<ServerSentEvent<String>> CHAT_EVENT_TYPE =
            new ParameterizedTypeReference<>() { };

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public AgentProviderType providerType() {
        return AgentProviderType.DIFY;
    }

    @Override
    public AgentProviderResult invoke(AgentConfig definition, AgentProviderInvocation invocation) {
        var request = invocation.request();
        ObjectNode body = objectMapper.createObjectNode();
        body.set("inputs", objectMapper.valueToTree(request.variables() == null ? Map.of() : request.variables()));
        body.put("response_mode", responseMode(definition.getInvocationMode()));
        body.put("user", invocation.userId());
        if (definition.getInvocationMode() == AgentInvocationMode.CHAT) {
            body.put("query", invocation.message());
            if (StringUtils.hasText(invocation.providerConversationId())) {
                body.put("conversation_id", invocation.providerConversationId());
            }
        }
        if (definition.getInvocationMode() == AgentInvocationMode.CHAT) {
            return invokeChat(definition, invocation, body);
        }
        JsonNode response = webClientBuilder.build().post().uri(endpoint(definition))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + definition.getCredential())
                .header(AgentApiHeaders.IDEMPOTENCY_KEY, invocation.invocationId())
                .bodyValue(body).retrieve().bodyToMono(JsonNode.class)
                .block(ProviderJsonSupport.timeout(objectMapper, definition));
        String text = extractWorkflowText(response);
        if (!StringUtils.hasText(text)) throw new BizException("Dify 应用未返回有效文本");
        JsonNode metadata = response == null ? null : response.get("metadata");
        JsonNode usage = metadata == null ? null : metadata.get("usage");
        return new AgentProviderResult(text,
                firstInteger(usage, "prompt_tokens", "input_tokens"),
                firstInteger(usage, "completion_tokens", "output_tokens"),
                null);
    }

    /** Dify CHAT 直接转发上游增量事件，工作流保持单次完成结果。 */
    @Override
    public Flux<AgentProviderStreamEvent> stream(
            AgentConfig definition, AgentProviderInvocation invocation) {
        if (definition.getInvocationMode() != AgentInvocationMode.CHAT) {
            return AgentProviderClient.super.stream(definition, invocation);
        }
        return Flux.defer(() -> streamChat(definition, invocation));
    }

    private Flux<AgentProviderStreamEvent> streamChat(
            AgentConfig definition, AgentProviderInvocation invocation) {
        ObjectNode body = chatBody(invocation);
        DifyChatStreamAccumulator accumulator = new DifyChatStreamAccumulator(objectMapper);
        AtomicBoolean completed = new AtomicBoolean();
        Flux<AgentProviderStreamEvent> events = webClientBuilder.build().post().uri(endpoint(definition))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + definition.getCredential())
                .header(AgentApiHeaders.IDEMPOTENCY_KEY, invocation.invocationId())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(CHAT_EVENT_TYPE)
                .transform(DifyAgentClient::chatEventData)
                .handle((payload, sink) -> {
                    JsonNode event = parseEvent(payload);
                    accumulator.accept(event);
                    String eventType = ProviderJsonSupport.text(event, "event");
                    String answer = ProviderJsonSupport.text(event, "answer");
                    switch (eventType == null ? "" : eventType) {
                        case "message", "agent_message" -> {
                            if (answer != null && !answer.isEmpty()) {
                                sink.next(AgentProviderStreamEvent.delta(answer));
                            }
                        }
                        case "message_replace" -> {
                            if (answer != null) sink.next(AgentProviderStreamEvent.replace(answer));
                        }
                        case "message_end" -> {
                            completed.set(true);
                            sink.next(AgentProviderStreamEvent.complete(accumulator.result()));
                        }
                        default -> {
                            // 思考、文件、TTS 与保活事件不属于对外回答文本。
                        }
                    }
                });
        return events.concatWith(Mono.defer(() -> completed.get()
                ? Mono.empty()
                : Mono.error(new BizException("Dify 流式响应未正常结束"))));
    }

    private ObjectNode chatBody(AgentProviderInvocation invocation) {
        var request = invocation.request();
        ObjectNode body = objectMapper.createObjectNode();
        body.set("inputs", objectMapper.valueToTree(request.variables() == null ? Map.of() : request.variables()));
        body.put("response_mode", "streaming");
        body.put("user", invocation.userId());
        body.put("query", invocation.message());
        if (StringUtils.hasText(invocation.providerConversationId())) {
            body.put("conversation_id", invocation.providerConversationId());
        }
        return body;
    }

    private JsonNode parseEvent(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JacksonException exception) {
            throw new BizException("Dify 流式响应不是合法 JSON");
        }
    }

    private AgentProviderResult invokeChat(
            AgentConfig definition,
            AgentProviderInvocation invocation,
            ObjectNode body) {
        DifyChatStreamAccumulator result = webClientBuilder.build().post().uri(endpoint(definition))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + definition.getCredential())
                .header(AgentApiHeaders.IDEMPOTENCY_KEY, invocation.invocationId())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(CHAT_EVENT_TYPE)
                .transform(DifyAgentClient::chatEventData)
                .reduce(new DifyChatStreamAccumulator(objectMapper), DifyChatStreamAccumulator::accept)
                .block(ProviderJsonSupport.timeout(objectMapper, definition));
        if (result == null) throw new BizException("Dify 应用未返回流式事件");
        return result.result();
    }

    /**
     * 提取携带有效正文的 Dify SSE 事件。
     *
     * <p>Dify 可能发送仅包含事件类型的保活帧；必须先过滤空 data，再进入 Reactor 的非空映射链。</p>
     */
    static Flux<String> chatEventData(Flux<ServerSentEvent<String>> events) {
        return events.filter(event -> event != null && StringUtils.hasText(event.data()))
                .map(ServerSentEvent::data);
    }

    /** Dify Agent Chat 只支持 streaming，工作流使用 blocking。 */
    static String responseMode(AgentInvocationMode mode) {
        return mode == AgentInvocationMode.CHAT ? "streaming" : "blocking";
    }

    private String endpoint(AgentConfig definition) {
        String endpoint = definition.getEndpointUrl().replaceAll("/+$", "");
        if (endpoint.endsWith("/chat-messages") || endpoint.endsWith("/workflows/run")) return endpoint;
        String suffix = definition.getInvocationMode() == AgentInvocationMode.WORKFLOW
                ? "/workflows/run" : "/chat-messages";
        return endpoint.endsWith("/v1") ? endpoint + suffix : endpoint + "/v1" + suffix;
    }

    private String extractWorkflowText(JsonNode response) {
        if (response == null) return null;
        JsonNode data = response.get("data");
        JsonNode outputs = data == null ? null : data.get("outputs");
        if (outputs == null || !outputs.isObject()) return null;
        for (String key : new String[]{"text", "answer", "result", "output"}) {
            String value = ProviderJsonSupport.text(outputs, key);
            if (StringUtils.hasText(value)) return value;
        }
        try {
            return objectMapper.writeValueAsString(outputs);
        } catch (Exception exception) {
            return null;
        }
    }

    private Integer firstInteger(JsonNode value, String first, String second) {
        Integer result = ProviderJsonSupport.integer(value, first);
        return result == null ? ProviderJsonSupport.integer(value, second) : result;
    }
}
