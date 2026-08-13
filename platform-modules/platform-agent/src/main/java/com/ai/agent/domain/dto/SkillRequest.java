package com.ai.agent.domain.dto;

import com.ai.agent.domain.SkillActionType;
import com.ai.agent.domain.SkillDisplayMode;
import com.ai.agent.domain.SkillJumpType;
import com.ai.agent.domain.SkillTerminalType;
import com.ai.agent.domain.SkillType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 创建或更新通用技能的请求。 */
public record SkillRequest(
        @NotBlank @Size(max = 500) String desc,
        @NotNull SkillActionType actionType,
        @NotBlank @Size(max = 128) String actionTitle,
        @NotBlank String actionPrompt,
        @NotNull SkillType type,
        @Size(max = 32) String scopeTypeCode,
        @NotNull SkillTerminalType terminalType,
        @Size(max = 500) String jumpUrl,
        SkillJumpType jumpType,
        SkillDisplayMode displayMode,
        @Min(0) @Max(100000) Integer sortOrder,
        @Min(0) @Max(100000) Integer weight,
        @Size(max = 500) String remark
) {
}
