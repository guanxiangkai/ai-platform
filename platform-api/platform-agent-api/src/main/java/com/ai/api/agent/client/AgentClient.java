package com.ai.api.agent.client;

import com.ai.api.agent.dto.AgentInvokeRequest;
import com.ai.api.agent.dto.AgentInvokeResult;
import reactor.core.publisher.Mono;

/**
 * 产品服务调用通用 Agent 的稳定契约。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface AgentClient {

    /**
     * 按稳定编码调用当前租户已发布的智能体。
     *
     * @param agentCode 智能体稳定编码
     * @param idempotencyKey 本次调用的稳定幂等键；同一业务调用重试时必须保持一致
     * @param request 产品无关的消息、上下文引用和变量
     * @return 调用结果
     */
    Mono<AgentInvokeResult> invoke(String agentCode, String idempotencyKey, AgentInvokeRequest request);
}
