package com.ai.scheduler.controller;

import com.ai.scheduler.service.SchedulerTaskService;
import com.ai.scheduler.web.SchedulerRunRequest;
import com.ai.scheduler.web.SchedulerTaskRequest;
import com.ai.scheduler.web.SchedulerViews;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
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

import java.util.List;

/** 面向各产品管理端的通用定时任务控制面接口。 */
@RestController
@RequestMapping({"/scheduler", "/api/scheduler"})
@RequiredArgsConstructor
@Tag(name = "通用定时任务", description = "租户任务定义、PowerJob 同步、手工触发和执行记录")
public class SchedulerController {

    private final SchedulerTaskService service;

    /** 查询当前租户允许使用的应用和处理器。 */
    @GetMapping("/applications")
    @RequiresPermission("scheduler:task:list")
    @Operation(summary = "查询应用与处理器目录")
    public ApiResponse<List<SchedulerViews.Application>> applications() {
        return ApiResponse.ok(service.applications());
    }

    /** 分页查询当前租户任务。 */
    @GetMapping("/tasks")
    @RequiresPermission("scheduler:task:list")
    @Operation(summary = "分页查询定时任务")
    public ApiResponse<PageResponse<SchedulerViews.Task>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(service.list(page, size, keyword, enabled));
    }

    /** 查询任务详情。 */
    @GetMapping("/tasks/{id}")
    @RequiresPermission("scheduler:task:query")
    @Operation(summary = "查询定时任务详情")
    public ApiResponse<SchedulerViews.Task> detail(@PathVariable String id) {
        return ApiResponse.ok(service.detail(id));
    }

    /** 创建任务。 */
    @PostMapping("/tasks")
    @RequiresPermission("scheduler:task:add")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class,
            typeCode = OperationTypes.INSERT, module = "通用定时任务", description = "创建定时任务")
    @Operation(summary = "创建定时任务")
    public ApiResponse<SchedulerViews.Task> create(@Valid @RequestBody SchedulerTaskRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    /** 更新任务。 */
    @PutMapping("/tasks/{id}")
    @RequiresPermission("scheduler:task:edit")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class,
            typeCode = OperationTypes.UPDATE, module = "通用定时任务", description = "更新定时任务")
    @Operation(summary = "更新定时任务")
    public ApiResponse<SchedulerViews.Task> update(@PathVariable String id,
                                                   @Valid @RequestBody SchedulerTaskRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    /** 修改任务启用状态。 */
    @PutMapping("/tasks/{id}/enabled")
    @RequiresPermission("scheduler:task:changeStatus")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class,
            typeCode = OperationTypes.UPDATE, module = "通用定时任务", description = "修改定时任务状态")
    @Operation(summary = "修改任务启用状态")
    public ApiResponse<SchedulerViews.Task> changeEnabled(@PathVariable String id,
                                                          @RequestParam boolean enabled) {
        return ApiResponse.ok(service.changeEnabled(id, enabled));
    }

    /** 重试同步到 PowerJob。 */
    @PostMapping("/tasks/{id}/sync")
    @RequiresPermission("scheduler:task:sync")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class,
            typeCode = OperationTypes.UPDATE, module = "通用定时任务", description = "同步定时任务")
    @Operation(summary = "同步任务到 PowerJob")
    public ApiResponse<SchedulerViews.Task> synchronize(@PathVariable String id) {
        return ApiResponse.ok(service.synchronize(id));
    }

    /** 手工触发一次任务。 */
    @PostMapping("/tasks/{id}/run")
    @RequiresPermission("scheduler:task:run")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class,
            typeCode = OperationTypes.UPDATE, module = "通用定时任务", description = "手工执行定时任务")
    @Operation(summary = "手工执行一次任务")
    public ApiResponse<Long> run(@PathVariable String id,
                                 @Valid @RequestBody(required = false) SchedulerRunRequest request) {
        return ApiResponse.ok(service.run(id, request == null ? null : request.parameters()));
    }

    /** 查询任务执行实例。 */
    @GetMapping("/tasks/{id}/instances")
    @RequiresPermission("scheduler:instance:list")
    @Operation(summary = "查询任务执行记录")
    public ApiResponse<PageResponse<SchedulerViews.Instance>> instances(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.instances(id, page, size));
    }

    /** 删除任务。 */
    @DeleteMapping("/tasks/{id}")
    @RequiresPermission("scheduler:task:delete")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class,
            typeCode = OperationTypes.DELETE, module = "通用定时任务", description = "删除定时任务")
    @Operation(summary = "删除定时任务")
    public ApiResponse<Boolean> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok(Boolean.TRUE);
    }
}
