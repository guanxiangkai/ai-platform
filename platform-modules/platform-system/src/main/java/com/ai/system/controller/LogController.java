package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresPermission;
import com.ai.system.domain.dto.LoginLogPageDTO;
import com.ai.system.domain.dto.OperationLogPageDTO;
import com.ai.system.domain.dto.OssLogPageDTO;
import com.ai.system.domain.vo.*;
import com.ai.system.service.ILoginLogService;
import com.ai.system.service.IOperationLogService;
import com.ai.system.service.IOssLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统一日志管理控制器
 * <p>
 * 聚合登录日志、操作日志、文件上传日志三类日志的查询与管理接口：
 * GET    /system/log/login/list              登录日志分页
 * GET    /system/log/login/{id}              登录日志详情
 * DELETE /system/log/login/clear             清空登录日志
 * GET    /system/log/operation/list          操作日志分页
 * GET    /system/log/operation/{id}          操作日志详情
 * GET    /system/log/operation/statistics    操作日志统计
 * DELETE /system/log/operation/clear         清空操作日志
 * GET    /system/log/upload/list             上传日志分页
 * GET    /system/log/upload/{id}             上传日志详情
 * DELETE /system/log/upload/clear            清空上传日志
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "统一日志管理", description = "登录日志 / 操作日志 / 文件上传日志查询与管理")
@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
public class LogController {

    private final ILoginLogService loginLogService;
    private final IOperationLogService operationLogService;
    private final IOssLogService uploadLogService;

    // ═══════════════════ 登录日志 ═══════════════════

    @RequiresPermission("system:log:list")
    @Operation(summary = "登录日志分页列表")
    @GetMapping("/login/list")
    public ApiResponse<PageResponse<LoginLogPageVO>> loginList(LoginLogPageDTO pageDTO) {
        return ApiResponse.ok(loginLogService.list(pageDTO));
    }

    @RequiresPermission("system:log:query")
    @Operation(summary = "登录日志详情")
    @GetMapping("/login/{id}")
    public ApiResponse<LoginLogVO> loginDetail(@PathVariable String id) {
        return ApiResponse.ok(loginLogService.detail(id));
    }

    @RequiresPermission("system:log:clear")
    @Operation(summary = "清空登录日志", description = "清空登录日志，保留最近 30 天数据。")
    @DeleteMapping("/login/clear")
    public ApiResponse<Boolean> loginClear() {
        return ApiResponse.ok(loginLogService.clear());
    }

    // ═══════════════════ 操作日志 ═══════════════════

    @RequiresPermission("system:log:list")
    @Operation(summary = "操作日志分页列表")
    @GetMapping("/operation/list")
    public ApiResponse<PageResponse<OperationLogPageVO>> operationList(OperationLogPageDTO pageDTO) {
        return ApiResponse.ok(operationLogService.list(pageDTO));
    }

    @RequiresPermission("system:log:query")
    @Operation(summary = "操作日志详情")
    @GetMapping("/operation/{id}")
    public ApiResponse<OperationLogVO> operationDetail(@PathVariable String id) {
        return ApiResponse.ok(operationLogService.detail(id));
    }

    @RequiresPermission("system:log:list")
    @Operation(summary = "操作日志统计", description = "统计指定时间范围内的操作日志数量及状态分布。")
    @GetMapping("/operation/statistics")
    public ApiResponse<Map<String, Object>> operationStatistics(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResponse.ok(operationLogService.statistics(startTime, endTime));
    }

    @RequiresPermission("system:log:clear")
    @Operation(summary = "清空操作日志", description = "清空操作日志，保留最近 30 天数据。")
    @DeleteMapping("/operation/clear")
    public ApiResponse<Boolean> operationClear() {
        return ApiResponse.ok(operationLogService.clear());
    }

    // ═══════════════════ 文件上传日志 ═══════════════════

    @RequiresPermission("system:log:list")
    @Operation(summary = "文件上传日志分页列表")
    @GetMapping("/oss/list")
    public ApiResponse<PageResponse<OssLogPageVO>> ossList(OssLogPageDTO pageDTO) {
        return ApiResponse.ok(uploadLogService.list(pageDTO));
    }

    @RequiresPermission("system:log:query")
    @Operation(summary = "文件上传日志详情")
    @GetMapping("/oss/{id}")
    public ApiResponse<OssLogVO> ossDetail(@PathVariable String id) {
        return ApiResponse.ok(uploadLogService.detail(id));
    }

    @RequiresPermission("system:log:clear")
    @Operation(summary = "清空上传日志", description = "清空文件上传日志，保留最近 30 天数据。")
    @DeleteMapping("/oss/clear")
    public ApiResponse<Boolean> ossClear() {
        return ApiResponse.ok(uploadLogService.clear());
    }
}
