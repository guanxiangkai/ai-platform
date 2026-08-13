package com.ai.api.agent;

/**
 * 智能体服务 HTTP 协议头。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class AgentApiHeaders {

    /** 幂等请求标识头。 */
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private AgentApiHeaders() {
    }
}
