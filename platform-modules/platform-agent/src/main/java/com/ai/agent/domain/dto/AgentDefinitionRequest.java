package com.ai.agent.domain.dto;

import com.ai.agent.domain.AgentInvocationMode;
import com.ai.agent.domain.AgentProviderType;
import com.ai.agent.domain.AgentPublishState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 创建或更新通用智能体定义时的期望配置。 */
@Schema(description = "通用智能体定义")
public record AgentDefinitionRequest(
        @NotBlank
        @Size(max = 128)
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_.-]{2,127}",
                message = "智能体编码需以字母开头，且只能包含字母、数字、点、下划线和短横线")
        String agentCode,

        @NotBlank
        @Size(max = 256)
        String agentName,

        @Size(max = 1000)
        String description,

        @NotNull
        AgentProviderType providerType,

        @NotNull
        AgentInvocationMode invocationMode,

        @NotBlank
        @Size(max = 1000)
        String endpointUrl,

        @Size(max = 256)
        String modelName,

        @Size(max = 2000)
        @Schema(description = "提供方密钥；更新时留空表示保留原密钥", accessMode = Schema.AccessMode.WRITE_ONLY)
        String credential,

        @Size(max = 32000)
        String systemPrompt,

        @DecimalMin("0.0")
        @DecimalMax("2.0")
        Double temperature,

        @Size(max = 32000)
        @Schema(description = "提供方扩展配置 JSON；只能包含产品无关的协议参数")
        String runtimeConfig,

        AgentPublishState publishState,

        Boolean enabled,

        @Size(max = 500)
        String remark
) {
}
