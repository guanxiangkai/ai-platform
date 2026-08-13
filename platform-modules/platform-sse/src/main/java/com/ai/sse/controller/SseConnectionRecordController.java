package com.ai.sse.controller;

import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresPermission;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import com.ai.sse.domain.dto.SseConnectionRecordDTO;
import com.ai.sse.domain.dto.SseConnectionRecordPageDTO;
import com.ai.sse.domain.entity.SseConnectionRecord;
import com.ai.sse.domain.vo.SseConnectionRecordPageVO;
import com.ai.sse.domain.vo.SseConnectionRecordVO;
import com.ai.sse.service.ISseConnectionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SSE 连接记录管理控制器
 * <p>
 * 继承 {@link BaseController} 获得标准 CRUD 接口（分页查询、详情、删除等），
 * 同时提供连接统计和历史清理等扩展功能。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "SSE 连接记录", description = "SSE 连接历史记录查询与管理")
@RestController
@RequestMapping("/sse/connection-record")
@RequiredArgsConstructor
public class SseConnectionRecordController extends BaseController<SseConnectionRecordPageDTO,
        SseConnectionRecordPageVO, SseConnectionRecordVO, SseConnectionRecordDTO,
        SseConnectionRecordDTO, SseConnectionRecord> {

    private final ISseConnectionRecordService service;

    @Override
    protected IBaseService<SseConnectionRecordPageDTO, SseConnectionRecordPageVO, SseConnectionRecordVO, SseConnectionRecordDTO, SseConnectionRecordDTO, SseConnectionRecord> getService() {
        return this.service;
    }

    @Override
    public String getPermissionPrefix() { return "sse:connectionRecord"; }

    @Override
    public String getModuleName() { return "Sse"; }

    @Override
    public String getEntityName() { return "SSE 连接记录"; }

    // ==================== 扩展接口 ====================

    /**
     * 获取连接统计信息
     */
    @GetMapping("/stats")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '连接统计'}")
    @Operation(summary = "连接统计", description = "查看各状态连接数")
    @RequiresPermission("sse:connectionRecord:stats")
    public ApiResponse<Map<String, Object>> getConnectionStats() {
        return ApiResponse.ok(service.getConnectionStats());
    }

    /**
     * 清理历史连接记录
     */
    @PostMapping("/cleanup")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.DELETE, module = "#{getModuleName()}", description = "#{getEntityName() + '历史清理'}")
    @Operation(summary = "清理历史记录", description = "清理指定天数之前的连接记录")
    @RequiresPermission("sse:connectionRecord:cleanup")
    public ApiResponse<Integer> cleanupHistory(
            @Parameter(description = "保留天数（默认30天）")
            @RequestParam(defaultValue = "30") int retainDays) {
        return ApiResponse.ok(service.cleanupHistory(retainDays));
    }
}
