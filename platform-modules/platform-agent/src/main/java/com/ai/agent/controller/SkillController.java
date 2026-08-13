package com.ai.agent.controller;

import com.ai.agent.domain.SkillTerminalType;
import com.ai.agent.domain.dto.SkillRequest;
import com.ai.agent.domain.vo.SkillViews;
import com.ai.agent.service.SkillManagementService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresLogin;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通用 AI 技能 API。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RequiresLogin
@Tag(name = "通用技能", description = "跨产品复用的技能展示与管理")
@RestController
@RequestMapping("/agent/skill")
@RequiredArgsConstructor
public class SkillController {
    private final SkillManagementService management;

    /** 分页查询技能。 */
    @RequiresPermission("agent:skill:list")
    @Operation(summary = "技能分页")
    @GetMapping("/list")
    public ApiResponse<PageResponse<SkillViews.Item>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String terminalType) {
        return ApiResponse.ok(management.page(pageNum, pageSize, title, type, terminalType));
    }

    /** 查询当前终端可展示的技能。 */
    @Operation(summary = "可展示技能")
    @GetMapping("/display")
    public ApiResponse<List<SkillViews.Item>> display(
            @RequestParam(required = false) String terminalType) {
        String resolvedTerminalType = StringUtils.hasText(terminalType)
                ? terminalType
                : SkillTerminalType.WEBSITE.code();
        return ApiResponse.ok(management.display(resolvedTerminalType));
    }

    /** 查询技能详情。 */
    @RequiresPermission("agent:skill:query")
    @Operation(summary = "技能详情")
    @GetMapping("/{id}")
    public ApiResponse<SkillViews.Item> detail(@PathVariable String id) {
        return ApiResponse.ok(management.detail(id));
    }

    /** 创建技能。 */
    @RequiresPermission("agent:skill:add")
    @OperationLog(typeCode = OperationTypes.INSERT, module = "通用技能", description = "创建技能")
    @Operation(summary = "创建技能")
    @PostMapping
    public ApiResponse<SkillViews.Item> create(@Valid @RequestBody SkillRequest request) {
        return ApiResponse.ok(management.create(request));
    }

    /** 更新技能。 */
    @RequiresPermission("agent:skill:edit")
    @OperationLog(typeCode = OperationTypes.UPDATE, module = "通用技能", description = "更新技能")
    @Operation(summary = "更新技能")
    @PutMapping("/{id}")
    public ApiResponse<SkillViews.Item> update(
            @PathVariable String id, @Valid @RequestBody SkillRequest request) {
        return ApiResponse.ok(management.update(id, request));
    }

    /** 删除技能。 */
    @RequiresPermission("agent:skill:delete")
    @OperationLog(typeCode = OperationTypes.DELETE, module = "通用技能", description = "删除技能")
    @Operation(summary = "删除技能")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable String id) {
        return ApiResponse.ok(management.delete(id));
    }

    /** 置顶技能。 */
    @RequiresPermission("agent:skill:pin")
    @OperationLog(typeCode = OperationTypes.UPDATE, module = "通用技能", description = "置顶技能")
    @Operation(summary = "置顶技能")
    @PostMapping("/{id}/pin")
    public ApiResponse<SkillViews.Item> pin(@PathVariable String id) {
        return ApiResponse.ok(management.pin(id));
    }

    /** 取消置顶技能。 */
    @RequiresPermission("agent:skill:unpin")
    @OperationLog(typeCode = OperationTypes.UPDATE, module = "通用技能", description = "取消置顶技能")
    @Operation(summary = "取消置顶技能")
    @PostMapping("/{id}/unpin")
    public ApiResponse<SkillViews.Item> unpin(@PathVariable String id) {
        return ApiResponse.ok(management.unpin(id));
    }
}
