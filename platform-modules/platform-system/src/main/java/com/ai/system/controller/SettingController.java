package com.ai.system.controller;

import com.ai.system.domain.dto.SettingDTO;
import com.ai.system.domain.vo.SettingVO;
import com.ai.system.service.ISettingService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前用户的平台设置接口。 */
@Tag(name = "应用设置", description = "当前用户的通用界面、通知、安全和产品扩展偏好")
@RestController
@RequestMapping("/system/setting")
@RequiredArgsConstructor
public class SettingController {

    private final ISettingService service;

    /** 查询当前登录用户的完整设置。 */
    @Operation(summary = "查询当前用户设置")
    @GetMapping("/current")
    public ApiResponse<SettingVO> getCurrentUserSetting() {
        return ApiResponse.ok(service.getCurrentUserSetting());
    }

    /** 按非空字段合并并保存当前登录用户的设置。 */
    @OperationLog(
            entity = com.ai.api.system.log.PlatformOperationLog.class,
            typeCode = OperationTypes.UPDATE,
            module = "系统设置",
            description = "保存当前用户系统设置")
    @Operation(summary = "保存当前用户设置")
    @PutMapping("/current")
    public ApiResponse<SettingVO> saveCurrentUserSetting(@Valid @RequestBody SettingDTO settingDTO) {
        return ApiResponse.ok(service.saveCurrentUserSetting(settingDTO));
    }
}
