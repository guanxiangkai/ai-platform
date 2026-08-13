package com.ai.agent.integration;

import com.ai.agent.domain.dto.AgentInvocationRequest;

import java.util.List;

/**
 * 提供方调用所需的已校验上下文。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public record AgentProviderInvocation(
        AgentInvocationRequest request,
        String userId,
        String invocationId,
        String providerConversationId,
        List<AgentProviderMessage> history
) {
    /** 固化历史消息，防止适配器调用期间被外部修改。 */
    public AgentProviderInvocation {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
