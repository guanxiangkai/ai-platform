package com.ai.api.agent.dto;

import java.io.Serial;
import java.io.Serializable;

/** 通用 Agent 调用结果。 */
public record AgentInvokeResult(
        String sessionId,
        String invocationId,
        String messageId,
        String text,
        Integer inputTokens,
        Integer outputTokens
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
