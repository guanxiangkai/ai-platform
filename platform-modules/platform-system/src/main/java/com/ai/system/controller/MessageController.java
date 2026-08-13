package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import com.ai.system.domain.dto.MessageCreateDTO;
import com.ai.system.domain.dto.MessageDTO;
import com.ai.system.domain.dto.MessagePageDTO;
import com.ai.system.domain.entity.Message;
import com.ai.system.domain.vo.MessagePageVO;
import com.ai.system.domain.vo.MessageVO;
import com.ai.system.service.IMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息管理控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "消息管理", description = "消息的增删改查、发送、已读等功能")
@RestController
@RequestMapping("/system/message")
@RequiredArgsConstructor
public class MessageController extends BaseController<MessagePageDTO, MessagePageVO, MessageVO, MessageCreateDTO, MessageDTO, Message> {

    private final IMessageService service;

    @Override
    protected IBaseService<MessagePageDTO, MessagePageVO, MessageVO, MessageCreateDTO, MessageDTO, Message> getService() {
        return this.service;
    }

    /**
     * 标记消息为已读
     *
     * @param id 消息ID
     * @return 是否成功
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '标记已读'}")
    @Operation(summary = "标记消息为已读", description = "将指定消息标记为已读状态。")
    @PutMapping("/{id}/read")
    public ApiResponse<Boolean> markAsRead(@PathVariable String id) {
        return ApiResponse.ok(service.markAsRead(id));
    }

    /**
     * 批量标记消息为已读
     *
     * @param ids 消息ID列表
     * @return 是否成功
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '批量标记已读'}")
    @Operation(summary = "批量标记消息为已读", description = "批量将消息标记为已读状态。")
    @PutMapping("/batch/read")
    public ApiResponse<Boolean> batchMarkAsRead(@RequestBody List<String> ids) {
        return ApiResponse.ok(service.batchMarkAsRead(ids));
    }

    /**
     * 获取未读消息数量
     *
     * @return 未读消息数量
     */
    @Operation(summary = "获取未读消息数量", description = "获取当前用户的未读消息数量。")
    @GetMapping("/unreadCount")
    public ApiResponse<Long> getUnreadCount() {
        return ApiResponse.ok(service.getUnreadCount());
    }

    /**
     * 获取公告消息列表
     *
     * @return 公告消息列表
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '公告查询'}")
    @Operation(summary = "获取公告消息", description = "获取所有展示状态的消息，按创建时间倒序排列。")
    @GetMapping("/notices")
    public ApiResponse<List<MessageVO>> getNotices() {
        return ApiResponse.ok(service.getNotices());
    }

    /**
     * 设置消息展示状态
     *
     * @param id        消息ID
     * @param isDisplay 是否展示
     * @return 是否成功
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '设置展示状态'}")
    @Operation(summary = "设置消息展示状态", description = "控制消息是否在公告栏中展示。")
    @PutMapping("/{id}/display")
    public ApiResponse<Boolean> setDisplay(
            @PathVariable String id,
            @RequestParam Boolean isDisplay) {
        return ApiResponse.ok(service.setDisplay(id, isDisplay));
    }

    /**
     * 批量设置消息展示状态
     *
     * @param ids       消息ID列表
     * @param isDisplay 是否展示
     * @return 是否成功
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '批量设置展示状态'}")
    @Operation(summary = "批量设置消息展示状态", description = "批量控制消息是否在公告栏中展示。")
    @PutMapping("/batch/display")
    public ApiResponse<Boolean> batchSetDisplay(
            @RequestBody List<String> ids,
            @RequestParam Boolean isDisplay) {
        return ApiResponse.ok(service.batchSetDisplay(ids, isDisplay));
    }
}
