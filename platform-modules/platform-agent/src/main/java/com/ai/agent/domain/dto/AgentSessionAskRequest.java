package com.ai.agent.domain.dto;

import com.ai.agent.domain.AgentResponseMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户端通用智能体会话请求。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public record AgentSessionAskRequest(
        @Size(max = 128) String invocationId,
        @Size(max = 64) String sessionId,
        @NotBlank @Size(max = 20000) String content,
        @Size(max = 64) String skillId,
        @Size(max = 64) String scopeTag,
        boolean voiceInputActive,
        AgentResponseMode responseMode,
        Map<String, Object> variables
) {
    /** 固化可选变量，防止调用期间被外部修改。 */
    public AgentSessionAskRequest {
        variables = variables == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }
}
