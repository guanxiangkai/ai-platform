package com.ai.agent.controller;

import com.ai.agent.domain.dto.SkillAuthorizationRequest;
import com.ai.agent.domain.vo.SkillAuthorizationViews;
import com.ai.agent.service.SkillAuthorizationService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresLogin;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 通用技能授权管理 API。 */
@RequiresLogin
@Tag(name = "技能授权", description = "技能与智能体绑定及用户技能白名单")
@RestController
@RequestMapping("/agent/skill-authorization")
@RequiredArgsConstructor
public class SkillAuthorizationController {
    private final SkillAuthorizationService authorization;

    /** 查询当前租户的技能—智能体绑定关系。 */
    @RequiresPermission("agent:authorization:list")
    @Operation(summary = "技能智能体关系")
    @GetMapping("/relations")
    public ApiResponse<List<SkillAuthorizationViews.Relation>> relations() {
        return ApiResponse.ok(authorization.skillRelations());
    }

    /** 绑定技能与已发布智能体。 */
    @RequiresPermission("agent:authorization:edit")
    @OperationLog(typeCode = OperationTypes.UPDATE, module = "技能授权", description = "绑定技能智能体")
    @Operation(summary = "绑定技能智能体")
    @PutMapping("/relations/{skillId}")
    public ApiResponse<SkillAuthorizationViews.Relation> bind(
            @PathVariable String skillId, @Valid @RequestBody SkillAuthorizationRequest.BindAgent request) {
        return ApiResponse.ok(authorization.bindAgent(skillId, request));
    }

    /** 解除技能与智能体的绑定。 */
    @RequiresPermission("agent:authorization:edit")
    @OperationLog(typeCode = OperationTypes.UPDATE, module = "技能授权", description = "解除技能智能体绑定")
    @Operation(summary = "解除技能智能体绑定")
    @DeleteMapping("/relations/{skillId}")
    public ApiResponse<Boolean> unbind(@PathVariable String skillId) {
        return ApiResponse.ok(authorization.unbindAgent(skillId));
    }

    /** 查询用户技能白名单。 */
    @RequiresPermission("agent:authorization:list")
    @Operation(summary = "用户技能白名单")
    @GetMapping("/users/{userId}")
    public ApiResponse<List<String>> userSkills(@PathVariable String userId) {
        return ApiResponse.ok(authorization.userSkillIds(userId));
    }

    /** 替换用户技能白名单。 */
    @RequiresPermission("agent:authorization:edit")
    @OperationLog(typeCode = OperationTypes.UPDATE, module = "技能授权", description = "更新用户技能白名单")
    @Operation(summary = "替换用户技能白名单")
    @PutMapping("/users/{userId}")
    public ApiResponse<List<String>> replaceUserSkills(
            @PathVariable String userId,
            @Valid @RequestBody SkillAuthorizationRequest.ReplaceUserSkills request) {
        return ApiResponse.ok(authorization.replaceUserSkillIds(userId, request));
    }
}
