package com.ai.agent.domain;

/** 智能体对外调用形态。 */
public enum AgentInvocationMode {
    /** 多轮或单轮对话。 */
    CHAT,
    /** 输入变量驱动的工作流。 */
    WORKFLOW
}
