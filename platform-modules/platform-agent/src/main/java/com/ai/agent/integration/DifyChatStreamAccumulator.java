package com.ai.agent.integration;

import io.github.guanxiangkai.web.plus.error.exception.BizException;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Dify Chat SSE 响应聚合器。
 *
 * <p>仅拼接最终回答事件，不采集智能体思考过程；替换事件会覆盖此前增量内容。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
final class DifyChatStreamAccumulator {
    private static final int ERROR_MESSAGE_MAX_LENGTH = 240;

    private final ObjectMapper objectMapper;
    private final StringBuilder answer = new StringBuilder();
    private String conversationId;
    private Integer inputTokens;
    private Integer outputTokens;

    DifyChatStreamAccumulator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 接收单个 SSE data 载荷。 */
    DifyChatStreamAccumulator accept(String payload) {
        if (!StringUtils.hasText(payload)) return this;
        JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (JacksonException exception) {
            throw new BizException("Dify 流式响应不是合法 JSON");
        }

        return accept(event);
    }

    /** 接收已经完成 JSON 解析的单个 SSE data 载荷。 */
    DifyChatStreamAccumulator accept(JsonNode event) {
        if (event == null || event.isNull()) return this;
        String eventType = ProviderJsonSupport.text(event, "event");
        if (!StringUtils.hasText(eventType) || "ping".equals(eventType)) return this;
        String currentConversationId = ProviderJsonSupport.text(event, "conversation_id");
        if (StringUtils.hasText(currentConversationId)) conversationId = currentConversationId;

        switch (eventType) {
            case "message", "agent_message" -> appendAnswer(event);
            case "message_replace" -> replaceAnswer(event);
            case "message_end" -> readUsage(event);
            case "error" -> throw providerError(event);
            default -> {
                // agent_thought、message_file 与 TTS 事件不是最终文本，按协议忽略。
            }
        }
        return this;
    }

    /** 生成统一提供方结果。 */
    AgentProviderResult result() {
        String text = answer.toString();
        if (!StringUtils.hasText(text)) throw new BizException("Dify 应用未返回有效文本");
        return new AgentProviderResult(text, inputTokens, outputTokens, conversationId);
    }

    private void appendAnswer(JsonNode event) {
        JsonNode value = event.get("answer");
        if (value != null && !value.isNull() && value.isString()) answer.append(value.stringValue());
    }

    private void replaceAnswer(JsonNode event) {
        JsonNode value = event.get("answer");
        if (value == null || value.isNull() || !value.isString()) return;
        answer.setLength(0);
        answer.append(value.stringValue());
    }

    private void readUsage(JsonNode event) {
        JsonNode metadata = event.get("metadata");
        JsonNode usage = metadata == null ? null : metadata.get("usage");
        inputTokens = firstInteger(usage, "prompt_tokens", "input_tokens");
        outputTokens = firstInteger(usage, "completion_tokens", "output_tokens");
    }

    private Integer firstInteger(JsonNode value, String first, String second) {
        Integer result = ProviderJsonSupport.integer(value, first);
        return result == null ? ProviderJsonSupport.integer(value, second) : result;
    }

    private BizException providerError(JsonNode event) {
        String code = ProviderJsonSupport.text(event, "code");
        String message = ProviderJsonSupport.text(event, "message");
        String detail = StringUtils.hasText(message) ? message : "未知错误";
        if (detail.length() > ERROR_MESSAGE_MAX_LENGTH) {
            detail = detail.substring(0, ERROR_MESSAGE_MAX_LENGTH);
        }
        return new BizException("Dify 调用失败"
                + (StringUtils.hasText(code) ? " [" + code + "]" : "")
                + ": " + detail);
    }
}
