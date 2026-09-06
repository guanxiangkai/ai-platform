package com.ai.agent.integration;

import com.ai.agent.domain.dto.AgentInvocationRequest;

import java.util.List;

/**
 * 提供方调用所需的已校验上下文。
 *
 * @param request 用于幂等、持久化和会话管理的原始请求
 * @param message 实际发送给提供方的消息，可包含平台生成的可信上下文
 * @param userId 当前用户标识
 * @param invocationId 调用幂等标识
 * @param providerConversationId 提供方连续对话标识
 * @param history 受限历史消息
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public record AgentProviderInvocation(
        AgentInvocationRequest request,
        String message,
        String userId,
        String invocationId,
        String providerConversationId,
        List<AgentProviderMessage> history
) {
    /** 使用请求原始消息创建提供方上下文。 */
    public AgentProviderInvocation(
            AgentInvocationRequest request,
            String userId,
            String invocationId,
            String providerConversationId,
            List<AgentProviderMessage> history) {
        this(request, request.message(), userId, invocationId, providerConversationId, history);
    }

    /** 固化历史消息，防止适配器调用期间被外部修改。 */
    public AgentProviderInvocation {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
