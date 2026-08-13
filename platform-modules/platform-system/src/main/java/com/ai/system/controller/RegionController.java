package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.RegionDTO;
import com.ai.system.domain.dto.RegionPageDTO;
import com.ai.system.domain.entity.Region;
import com.ai.system.domain.vo.RegionPageVO;
import com.ai.system.domain.vo.RegionVO;
import com.ai.system.service.IRegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 行政区域管理控制器
 * <p>
 * 提供省/市/区/县/街道的树形层级管理
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "行政区域管理", description = "行政区域的增删改查、树形结构查询")
@RestController
@RequestMapping("/system/region")
@RequiredArgsConstructor
public class RegionController extends BaseController<RegionPageDTO, RegionPageVO, RegionVO, RegionDTO, RegionDTO, Region> {

    private final IRegionService service;

    @Override
    protected IBaseService<RegionPageDTO, RegionPageVO, RegionVO, RegionDTO, RegionDTO, Region> getService() {
        return this.service;
    }

    /**
     * 查询区域树
     */
    @Operation(summary = "查询区域树", description = "根据区域编码查询区域树，城市编码为空时返回完整树")
    @GetMapping("/tree")
    public ApiResponse<List<RegionVO>> tree(
            @Parameter(description = "区域编码（可选，不传则查询完整树）") @RequestParam(required = false) String code) {
        return ApiResponse.ok(service.tree(code));
    }

    /**
     * 查询区域下拉数据（子区域）
     */
    @Operation(summary = "查询子区域下拉框", description = "根据父区域编码查询直接子区域（懒加载），不传则查询全部区域")
    @GetMapping("/children")
    public ApiResponse<List<OptionItem>> children(
            @Parameter(description = "父区域编码（不传则查询全部区域）") @RequestParam(required = false) String code) {
        return ApiResponse.ok(service.children(code));
    }

    /**
     * 根据编码查询区域
     */
    @Operation(summary = "根据编码查询区域", description = "根据行政区划编码查询区域详情")
    @GetMapping("/code")
    public ApiResponse<RegionVO> getByCode(
            @Parameter(description = "区域编码", required = true) @RequestParam String code) {
        return ApiResponse.ok(service.getByCode(code));
    }
}
