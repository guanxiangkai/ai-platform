package com.ai.agent.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 按通用契约调用智能体。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "智能体调用请求")
public record AgentInvocationRequest(
        @Size(max = 128)
        @Schema(description = "调用幂等标识；同一租户内重复提交时复用既有调用结果")
        String invocationId,

        @Size(max = 64)
        @Schema(description = "已有会话 ID；留空时自动创建会话")
        String sessionId,

        @NotBlank
        @Size(max = 32000)
        String message,

        @Size(max = 256)
        String sessionTitle,

        @Size(max = 128)
        @Schema(description = "调用方自定义的上下文命名空间，不解释其业务含义")
        String contextNamespace,

        @Size(max = 256)
        @Schema(description = "调用方自定义的上下文引用，不建立跨服务外键")
        String contextReference,

        @Schema(description = "提供给工作流或模型的临时变量")
        Map<String, Object> variables
) {
}
