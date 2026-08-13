package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.DictCreateDTO;
import com.ai.system.domain.dto.DictDTO;
import com.ai.system.domain.dto.DictPageDTO;
import com.ai.system.domain.entity.Dict;
import com.ai.system.domain.vo.DictPageVO;
import com.ai.system.domain.vo.DictVO;
import com.ai.system.service.IDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "字典管理", description = "字典CRUD、类型管理等功能")
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class DictController extends BaseController<DictPageDTO, DictPageVO, DictVO, DictCreateDTO, DictDTO, Dict> {

    private final IDictService service;

    @Override
    protected IBaseService<DictPageDTO, DictPageVO, DictVO, DictCreateDTO, DictDTO, Dict> getService() {
        return this.service;
    }

    /**
     * 获取字典选项列表
     *
     * @return 字典选项列表
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '选项列表'}")
    @Operation(summary = "获取字典选项列表", description = "获取所有字典的选项列表，用于下拉选择等场景。")
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.ok(service.options());
    }

    /**
     * 字典排序
     *
     * @param ids 排序后的字典ID列表
     * @return 更新是否成功
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '排序'}")
    @Operation(summary = "字典排序", description = "调整字典显示顺序。")
    @PutMapping("/sort")
    public ApiResponse<Boolean> updateSort(@RequestBody List<String> ids) {
        return ApiResponse.ok(service.updateSort(ids));
    }

    /**
     * 刷新字典缓存
     *
     * @return 刷新是否成功
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '缓存刷新'}")
    @Operation(summary = "刷新字典缓存", description = "清除字典缓存，重新加载字典数据。")
    @PostMapping("/refresh")
    public ApiResponse<Boolean> refreshCache() {
        return ApiResponse.ok(service.refreshCache());
    }


}
