package com.ai.agent.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 通用技能授权管理的写入请求。 */
public final class SkillAuthorizationRequest {
    private SkillAuthorizationRequest() {
    }

    /** 将一个技能绑定到指定智能体。 */
    @Schema(description = "技能智能体绑定请求")
    public record BindAgent(
            @NotBlank @Size(max = 64) @Schema(description = "智能体ID") String agentId) {
    }

    /** 用完整技能集合替换一个用户的可用技能白名单。 */
    @Schema(description = "用户技能白名单替换请求")
    public record ReplaceUserSkills(
            @NotNull @Size(max = 1000) @Schema(description = "技能ID集合")
            List<@NotBlank @Size(max = 64) String> skillIds) {
    }
}
