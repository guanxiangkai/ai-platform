package com.ai.agent.controller;

import com.ai.agent.domain.dto.AgentInvocationRequest;
import com.ai.agent.domain.vo.AgentViews;
import com.ai.agent.service.AgentInvocationService;
import com.ai.api.agent.AgentApiHeaders;
import com.ai.api.agent.dto.AgentInvokeRequest;
import com.ai.api.agent.dto.AgentInvokeResult;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅供受信产品服务调用的通用 Agent 接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RestController
@RequestMapping("/internal/agent")
@RequiredArgsConstructor
public class InternalAgentController {
    private final AgentInvocationService invocationService;

    /**
     * 按当前租户内的稳定智能体编码执行调用。
     *
     * @param agentCode 智能体稳定编码
     * @param idempotencyKey 调用方生成并在重试中保持不变的幂等键
     * @param request 产品无关调用参数
     * @return 智能体调用结果
     */
    @PostMapping("/definitions/{agentCode}/invoke")
    public AgentInvokeResult invoke(
            @PathVariable String agentCode,
            @RequestHeader(AgentApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            @RequestBody AgentInvokeRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new BizException("调用消息不能为空");
        }
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BizException("幂等键不能为空");
        }
        AgentViews.InvocationResult result = invocationService.invokeByCode(
                agentCode,
                new AgentInvocationRequest(
                        idempotencyKey.trim(),
                        request.sessionId(),
                        request.message(),
                        request.sessionTitle(),
                        request.contextNamespace(),
                        request.contextReference(),
                        request.variables()
                )
        );
        return new AgentInvokeResult(
                result.sessionId(),
                result.invocationId(),
                result.messageId(),
                result.text(),
                result.inputTokens(),
                result.outputTokens()
        );
    }
}
