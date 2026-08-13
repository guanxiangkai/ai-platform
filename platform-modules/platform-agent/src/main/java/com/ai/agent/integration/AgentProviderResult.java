package com.ai.agent.integration;

/**
 * 提供方调用的统一结果。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public record AgentProviderResult(
        String text,
        Integer inputTokens,
        Integer outputTokens,
        String providerConversationId
) {
}
