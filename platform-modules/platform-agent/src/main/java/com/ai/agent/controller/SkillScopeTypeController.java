package com.ai.agent.controller;

import com.ai.agent.domain.vo.SkillViews;
import com.ai.agent.service.SkillManagementService;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 通用技能作用域 API。 */
@RequiresLogin
@Tag(name = "技能作用域", description = "跨产品复用的技能作用域类型")
@RestController
@RequestMapping("/agent/skill-scope-type")
@RequiredArgsConstructor
public class SkillScopeTypeController {
    private final SkillManagementService management;

    /** 查询启用且允许在输入框展示的作用域。 */
    @Operation(summary = "可展示技能作用域")
    @GetMapping("/display")
    public ApiResponse<List<SkillViews.ScopeType>> display() {
        return ApiResponse.ok(management.displayScopeTypes());
    }
}
