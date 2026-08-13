package com.ai.agent.controller;

import com.ai.agent.domain.AgentInvocationState;
import com.ai.agent.domain.AgentProviderType;
import com.ai.agent.domain.AgentPublishState;
import com.ai.agent.domain.AgentSessionState;
import com.ai.agent.domain.dto.AgentDefinitionRequest;
import com.ai.agent.domain.dto.AgentInvocationRequest;
import com.ai.agent.domain.vo.AgentViews;
import com.ai.agent.service.AgentInvocationService;
import com.ai.agent.service.AgentManagementService;
import com.ai.agent.service.SkillAuthorizationService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/** 通用智能体定义、会话和调用审计 API。 */
@RequiresLogin
@Tag(name = "通用智能体", description = "跨产品的智能体定义、调用和审计")
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentManagementController {
    private final AgentManagementService management;
    private final AgentInvocationService invocations;
    private final SkillAuthorizationService authorization;

    /** 查询技能授权面板可绑定的已发布智能体。 */
    @RequiresPermission("agent:authorization:list")
    @Operation(summary = "可绑定智能体选项")
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.ok(authorization.agentOptions());
    }

    /** 分页查询智能体定义。 */
    @RequiresPermission("agent:definition:list")
    @Operation(summary = "智能体定义分页")
    @GetMapping("/definitions")
    public ApiResponse<PageResponse<AgentViews.Definition>> definitions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AgentProviderType providerType,
            @RequestParam(required = false) AgentPublishState publishState,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(management.definitions(page, size, keyword, providerType, publishState, enabled));
    }

    /** 查询单个智能体定义。 */
    @RequiresPermission("agent:definition:query")
    @Operation(summary = "查询智能体定义")
    @GetMapping("/definitions/{id}")
    public ApiResponse<AgentViews.Definition> definition(@PathVariable String id) {
        return ApiResponse.ok(management.definition(id));
    }

    /** 创建智能体定义。 */
    @RequiresPermission("agent:definition:add")
    @OperationLog(typeCode = OperationTypes.INSERT, module = "通用智能体", description = "创建智能体定义")
    @Operation(summary = "创建智能体定义")
    @PostMapping("/definitions")
    public ApiResponse<AgentViews.Definition> create(@Valid @RequestBody AgentDefinitionRequest request) {
        return ApiResponse.ok(management.create(request));
    }

    /** 更新智能体定义。 */
    @RequiresPermission("agent:definition:edit")
    @OperationLog(typeCode = OperationTypes.UPDATE, module = "通用智能体", description = "更新智能体定义")
    @Operation(summary = "更新智能体定义")
    @PutMapping("/definitions/{id}")
    public ApiResponse<AgentViews.Definition> update(
            @PathVariable String id, @Valid @RequestBody AgentDefinitionRequest request) {
        return ApiResponse.ok(management.update(id, request));
    }

    /** 修改智能体启用状态。 */
    @RequiresPermission("agent:definition:edit")
    @OperationLog(typeCode = OperationTypes.UPDATE, module = "通用智能体", description = "修改智能体状态")
    @Operation(summary = "修改智能体启用状态")
    @PutMapping("/definitions/{id}/enabled")
    public ApiResponse<AgentViews.Definition> changeEnabled(
            @PathVariable String id, @RequestParam boolean enabled) {
        return ApiResponse.ok(management.changeEnabled(id, enabled));
    }

    /** 删除智能体定义。 */
    @RequiresPermission("agent:definition:delete")
    @OperationLog(typeCode = OperationTypes.DELETE, module = "通用智能体", description = "删除智能体定义")
    @Operation(summary = "删除智能体定义")
    @DeleteMapping("/definitions/{id}")
    public ApiResponse<Boolean> delete(@PathVariable String id) {
        return ApiResponse.ok(management.delete(id));
    }

    /** 使用真实上游测试草稿或已发布定义。 */
    @RequiresPermission("agent:definition:test")
    @OperationLog(typeCode = OperationTypes.OTHER, module = "通用智能体", description = "测试智能体定义")
    @Operation(summary = "测试智能体定义")
    @PostMapping("/definitions/{id}/test")
    public Mono<ApiResponse<AgentViews.InvocationResult>> test(
            @PathVariable String id, @Valid @RequestBody AgentInvocationRequest request) {
        return Mono.fromCallable(() -> ApiResponse.ok(invocations.test(id, request)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 调用已发布智能体。 */
    @RequiresPermission("agent:invoke")
    @Operation(summary = "调用已发布智能体")
    @PostMapping("/definitions/{id}/invoke")
    public Mono<ApiResponse<AgentViews.InvocationResult>> invoke(
            @PathVariable String id, @Valid @RequestBody AgentInvocationRequest request) {
        return Mono.fromCallable(() -> ApiResponse.ok(invocations.invoke(id, request)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 分页查询会话。 */
    @RequiresPermission("agent:session:list")
    @Operation(summary = "智能体会话分页")
    @GetMapping("/sessions")
    public ApiResponse<PageResponse<AgentViews.Session>> sessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) AgentSessionState state) {
        return ApiResponse.ok(management.sessionPage(page, size, keyword, agentId, state));
    }

    /** 查询会话与消息明细。 */
    @RequiresPermission("agent:session:query")
    @Operation(summary = "查询智能体会话")
    @GetMapping("/sessions/{id}")
    public ApiResponse<AgentViews.Session> session(@PathVariable String id) {
        return ApiResponse.ok(management.session(id));
    }

    /** 分页查询调用审计。 */
    @RequiresPermission("agent:invocation:list")
    @Operation(summary = "智能体调用审计分页")
    @GetMapping("/invocations")
    public ApiResponse<PageResponse<AgentViews.Invocation>> invocations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) AgentInvocationState state) {
        return ApiResponse.ok(management.invocationPage(page, size, keyword, agentId, state));
    }

    /** 分页查询语音转写审计。 */
    @RequiresPermission("agent:voice:list")
    @Operation(summary = "语音转写审计分页")
    @GetMapping("/voices")
    public ApiResponse<PageResponse<AgentViews.Voice>> voices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(management.voicePage(page, size, keyword, status));
    }
}
