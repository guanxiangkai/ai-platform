package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.TenantDTO;
import com.ai.system.domain.dto.TenantPageDTO;
import com.ai.system.domain.entity.Tenant;
import com.ai.system.domain.vo.TenantPageVO;
import com.ai.system.domain.vo.TenantVO;
import com.ai.system.service.ITenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户管理控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/system/tenant")
@RequiredArgsConstructor
@Tag(name = "租户管理", description = "租户的增删改查、状态管理等功能")
public class TenantController extends BaseController<TenantPageDTO, TenantPageVO, TenantVO, TenantDTO, TenantDTO, Tenant> {

    private final ITenantService service;

    @Override
    protected IBaseService<TenantPageDTO, TenantPageVO, TenantVO, TenantDTO, TenantDTO, Tenant> getService() {
        return this.service;
    }

    /**
     * 获取租户选项列表
     *
     * @return 租户选项列表
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '选项列表'}")
    @Operation(summary = "获取租户选项列表", description = "获取所有启用租户的选项列表，用于下拉选择等场景。")
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.ok(service.options());
    }

    /**
     * 检查租户编码是否存在
     *
     * @param tenantCode 租户编码
     * @return 是否存在
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '编码检查'}")
    @Operation(summary = "检查租户编码", description = "检查租户编码是否已存在")
    @GetMapping("/checkCode")
    public ApiResponse<Boolean> checkTenantCode(@RequestParam String tenantCode) {
        return ApiResponse.ok(service.checkTenantCode(tenantCode));
    }


}
