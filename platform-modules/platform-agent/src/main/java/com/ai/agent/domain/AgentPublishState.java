package com.ai.agent.domain;

/** 智能体配置的发布状态。 */
public enum AgentPublishState {
    /** 尚未进入运行入口的草稿。 */
    DRAFT,
    /** 可由业务系统按通用契约调用的已发布版本。 */
    PUBLISHED
}
