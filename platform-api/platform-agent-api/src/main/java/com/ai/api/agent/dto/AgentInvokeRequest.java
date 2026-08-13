package com.ai.api.agent.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 产品服务向通用 Agent 传递的协议输入。 */
public record AgentInvokeRequest(
        String sessionId,
        String message,
        String sessionTitle,
        String contextNamespace,
        String contextReference,
        Map<String, Object> variables
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 规范化可选变量，避免客户端与服务端处理空集合的方式不一致。 */
    public AgentInvokeRequest {
        variables = variables == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }
}
