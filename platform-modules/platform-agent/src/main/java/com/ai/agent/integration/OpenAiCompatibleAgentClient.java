package com.ai.agent.integration;

import com.ai.agent.domain.AgentMessageRole;
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
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * OpenAI Chat Completions 兼容协议适配器。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleAgentClient implements AgentProviderClient {
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public AgentProviderType providerType() {
        return AgentProviderType.OPENAI_COMPATIBLE;
    }

    @Override
    public AgentProviderResult invoke(AgentConfig definition, AgentProviderInvocation invocation) {
        var request = invocation.request();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", definition.getModelName());
        body.put("temperature", definition.getTemperature() == null ? 0.7D : definition.getTemperature());
        ArrayNode messages = body.putArray("messages");
        if (StringUtils.hasText(definition.getSystemPrompt())) {
            messages.addObject().put("role", "system").put("content", definition.getSystemPrompt());
        }
        invocation.history().forEach(message -> messages.addObject()
                .put("role", message.role())
                .put("content", message.content()));
        messages.addObject().put("role", AgentMessageRole.USER.value()).put("content", request.message());
        JsonNode response = webClientBuilder.build().post().uri(endpoint(definition.getEndpointUrl()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + definition.getCredential())
                .header(AgentApiHeaders.IDEMPOTENCY_KEY, invocation.invocationId())
                .bodyValue(body).retrieve().bodyToMono(JsonNode.class)
                .block(ProviderJsonSupport.timeout(objectMapper, definition));
        String text = choiceText(response);
        if (!StringUtils.hasText(text)) throw new BizException("OpenAI 兼容服务未返回有效文本");
        JsonNode usage = response == null ? null : response.get("usage");
        return new AgentProviderResult(text,
                ProviderJsonSupport.integer(usage, "prompt_tokens"),
                ProviderJsonSupport.integer(usage, "completion_tokens"),
                null);
    }

    private String endpoint(String value) {
        String endpoint = value.replaceAll("/+$", "");
        if (endpoint.endsWith("/chat/completions")) return endpoint;
        return endpoint.endsWith("/v1") ? endpoint + "/chat/completions" : endpoint + "/v1/chat/completions";
    }

    private String choiceText(JsonNode response) {
        if (response == null) return null;
        JsonNode choices = response.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) return null;
        JsonNode message = choices.get(0).get("message");
        return ProviderJsonSupport.text(message, "content");
    }
}
