package com.ai.agent.domain;

/**
 * 智能体会话中可持久化并发送给模型的消息角色。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum AgentMessageRole {
    USER("user"),
    ASSISTANT("assistant");

    private final String value;

    AgentMessageRole(String value) {
        this.value = value;
    }

    /** 返回持久化与提供方协议共用的角色值。 */
    public String value() {
        return value;
    }

    /** 判断字符串是否为可用于对话历史的角色。 */
    public static boolean contains(String value) {
        for (AgentMessageRole role : values()) {
            if (role.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
