package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.DictItemCreateDTO;
import com.ai.system.domain.dto.DictItemDTO;
import com.ai.system.domain.dto.DictItemPageDTO;
import com.ai.system.domain.entity.DictItem;
import com.ai.system.domain.vo.DictItemPageVO;
import com.ai.system.domain.vo.DictItemVO;
import com.ai.system.service.IDictItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典项管理控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "字典项管理", description = "字典项CRUD等功能")
@RestController
@RequestMapping("/system/dictItem")
@RequiredArgsConstructor
public class DictItemController extends BaseController<DictItemPageDTO, DictItemPageVO, DictItemVO, DictItemCreateDTO, DictItemDTO, DictItem> {

    private final IDictItemService service;

    @Override
    protected IBaseService<DictItemPageDTO, DictItemPageVO, DictItemVO, DictItemCreateDTO, DictItemDTO, DictItem> getService() {
        return this.service;
    }


    /**
     * 获取字典项选项列表
     *
     * @param dictCode 字典类型编码
     * @return 字典项选项列表
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '选项列表'}")
    @Operation(summary = "获取字典项选项列表", description = "根据字典类型编码获取字典项选项列表，用于下拉选择等场景。")
    @GetMapping("/type/{dictCode}")
    public ApiResponse<List<OptionItem>> getDictItemsByType(@PathVariable String dictCode) {
        return ApiResponse.ok(service.options(dictCode));
    }

    /**
     * 字典项排序
     *
     * @param ids 排序后的字典项ID列表
     * @return 更新是否成功
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '排序'}")
    @Operation(summary = "字典项排序", description = "调整字典项显示顺序。")
    @PutMapping("/sort")
    public ApiResponse<Boolean> updateSort(@RequestBody List<String> ids) {
        return ApiResponse.ok(service.updateSort(ids));
    }

    /**
     * 设置默认字典项（原子切换）
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '默认项设置'}")
    @Operation(summary = "设置默认字典项", description = "将指定字典项设为默认，并清空同字典代码下其他默认项。")
    @PutMapping("/{id}/default")
    public ApiResponse<Boolean> setDefault(@PathVariable String id) {
        return ApiResponse.ok(service.setDefault(id));
    }


}
