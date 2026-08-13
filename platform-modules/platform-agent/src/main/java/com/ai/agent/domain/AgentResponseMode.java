package com.ai.agent.domain;

/**
 * 用户端智能体回答的呈现模式。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum AgentResponseMode {
    /** 自然语言对话。 */
    CHAT,
    /** 供表格、卡片等界面继续解析的结构化回答。 */
    STRUCTURED
}
