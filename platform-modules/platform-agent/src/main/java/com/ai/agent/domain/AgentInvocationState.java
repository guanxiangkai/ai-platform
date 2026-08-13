package com.ai.agent.domain;

/**
 * 单次智能体调用状态。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum AgentInvocationState {
    RUNNING,
    PROVIDER_SUCCEEDED,
    SUCCEEDED,
    FAILED
}
