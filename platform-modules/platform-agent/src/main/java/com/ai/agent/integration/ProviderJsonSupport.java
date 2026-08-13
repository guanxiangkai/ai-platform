package com.ai.agent.integration;

import com.ai.agent.domain.entity.AgentConfig;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/** 提供方适配器共用的 JSON 与超时读取能力。 */
final class ProviderJsonSupport {
    private ProviderJsonSupport() {
    }

    /** 从运行扩展配置读取调用超时，默认 180 秒。 */
    static Duration timeout(ObjectMapper objectMapper, AgentConfig definition) {
        JsonNode config = config(objectMapper, definition);
        JsonNode value = config == null ? null : config.get("timeoutSeconds");
        int seconds = value == null || value.isNull() ? 180 : value.intValue();
        return Duration.ofSeconds(Math.min(600, Math.max(1, seconds)));
    }

    /** 解析运行扩展配置；保存时已完成对象校验。 */
    static JsonNode config(ObjectMapper objectMapper, AgentConfig definition) {
        if (!StringUtils.hasText(definition.getRuntimeConfig())) return null;
        try {
            return objectMapper.readTree(definition.getRuntimeConfig());
        } catch (JacksonException ignored) {
            return null;
        }
    }

    /** 读取文本节点。 */
    static String text(JsonNode parent, String key) {
        if (parent == null) return null;
        JsonNode value = parent.get(key);
        if (value == null || value.isNull()) return null;
        String text = value.stringValue();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    /** 读取整数节点。 */
    static Integer integer(JsonNode parent, String key) {
        if (parent == null) return null;
        JsonNode value = parent.get(key);
        return value == null || value.isNull() ? null : value.intValue();
    }
}
