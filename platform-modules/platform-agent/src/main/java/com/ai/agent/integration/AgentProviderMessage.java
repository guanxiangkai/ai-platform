package com.ai.agent.integration;

/**
 * 提供方调用边界中的历史消息。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public record AgentProviderMessage(String role, String content) {
}
