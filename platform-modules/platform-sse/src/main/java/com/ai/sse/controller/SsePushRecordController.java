package com.ai.sse.controller;

import io.github.guanxiangkai.web.plus.security.annotation.RequiresPermission;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import com.ai.sse.domain.dto.SsePushRecordDTO;
import com.ai.sse.domain.dto.SsePushRecordPageDTO;
import com.ai.sse.domain.entity.SsePushRecord;
import com.ai.sse.domain.vo.SsePushRecordPageVO;
import com.ai.sse.domain.vo.SsePushRecordVO;
import com.ai.sse.service.ISsePushRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SSE 推送日志管理控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "SSE 推送日志", description = "SSE 推送日志查询与管理")
@RestController
@RequestMapping("/sse/push-log")
@RequiredArgsConstructor
public class SsePushRecordController extends BaseController<SsePushRecordPageDTO, SsePushRecordPageVO, SsePushRecordVO, SsePushRecordDTO, SsePushRecordDTO, SsePushRecord> {

    private final ISsePushRecordService service;

    @Override
    protected IBaseService<SsePushRecordPageDTO, SsePushRecordPageVO, SsePushRecordVO, SsePushRecordDTO, SsePushRecordDTO, SsePushRecord> getService() {
        return this.service;
    }

    @Override
    public String getPermissionPrefix() { return "sse:pushRecord"; }

    @Override
    public String getModuleName() { return "Sse"; }

    @Override
    public String getEntityName() { return "SSE 推送日志"; }

    @Operation(summary = "清理历史推送日志", description = "清理指定天数之前的推送日志记录")
    @PostMapping("/cleanup")
    @RequiresPermission("sse:pushRecord:cleanup")
    public ApiResponse<Integer> cleanup(
            @Parameter(description = "保留天数（默认30天）")
            @RequestParam(defaultValue = "30") int retainDays) {
        return ApiResponse.ok(service.cleanupHistory(retainDays));
    }
}
