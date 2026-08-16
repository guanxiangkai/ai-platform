package com.ai.agent.integration;

import com.ai.agent.domain.AgentInvocationMode;
import com.ai.agent.domain.AgentProviderType;
import com.ai.agent.domain.entity.AgentConfig;
import com.ai.api.agent.AgentApiHeaders;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Dify 对话与工作流应用协议适配器。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class DifyAgentClient implements AgentProviderClient {
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
        body.put("response_mode", "blocking");
        body.put("user", invocation.userId());
        if (definition.getInvocationMode() == AgentInvocationMode.CHAT) {
            body.put("query", request.message());
            if (StringUtils.hasText(invocation.providerConversationId())) {
                body.put("conversation_id", invocation.providerConversationId());
            }
        }
        JsonNode response = webClientBuilder.build().post().uri(endpoint(definition))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + definition.getCredential())
                .header(AgentApiHeaders.IDEMPOTENCY_KEY, invocation.invocationId())
                .bodyValue(body).retrieve().bodyToMono(JsonNode.class)
                .block(ProviderJsonSupport.timeout(objectMapper, definition));
        String text = extractText(response, definition.getInvocationMode());
        if (!StringUtils.hasText(text)) throw new BizException("Dify 应用未返回有效文本");
        JsonNode metadata = response == null ? null : response.get("metadata");
        JsonNode usage = metadata == null ? null : metadata.get("usage");
        return new AgentProviderResult(text,
                firstInteger(usage, "prompt_tokens", "input_tokens"),
                firstInteger(usage, "completion_tokens", "output_tokens"),
                definition.getInvocationMode() == AgentInvocationMode.CHAT
                        ? ProviderJsonSupport.text(response, "conversation_id") : null);
    }

    private String endpoint(AgentConfig definition) {
        String endpoint = definition.getEndpointUrl().replaceAll("/+$", "");
        if (endpoint.endsWith("/chat-messages") || endpoint.endsWith("/workflows/run")) return endpoint;
        String suffix = definition.getInvocationMode() == AgentInvocationMode.WORKFLOW
                ? "/workflows/run" : "/chat-messages";
        return endpoint.endsWith("/v1") ? endpoint + suffix : endpoint + "/v1" + suffix;
    }

    private String extractText(JsonNode response, AgentInvocationMode mode) {
        if (response == null) return null;
        if (mode == AgentInvocationMode.CHAT) return ProviderJsonSupport.text(response, "answer");
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
