package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import com.ai.system.domain.dto.DeptDTO;
import com.ai.system.domain.dto.DeptPageDTO;
import com.ai.system.domain.entity.Dept;
import com.ai.system.domain.vo.DeptPageVO;
import com.ai.system.domain.vo.DeptVO;
import com.ai.system.domain.vo.RegionVO;
import com.ai.system.service.IDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门管理控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
@Tag(name = "部门管理", description = "部门的增删改查、树形结构等功能")
public class DeptController extends BaseController<DeptPageDTO, DeptPageVO, DeptVO, DeptDTO, DeptDTO, Dept> {

    private final IDeptService service;

    @Override
    protected IBaseService<DeptPageDTO, DeptPageVO, DeptVO, DeptDTO, DeptDTO, Dept> getService() {
        return this.service;
    }

    /**
     * 获取部门选择器列表
     * <p>
     * 根据当前用户的数据权限返回可选择的部门列表，用于页面部门下拉框
     *
     * @return 部门选择器列表
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '选择器列表'}")
    @Operation(summary = "获取部门选择器列表", description = "根据数据权限返回可选择的部门列表")
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.ok(service.options());
    }

    /**
     * 获取当前用户选中的部门
     *
     * @return 当前选中的部门
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "当前选中部门查询")
    @Operation(summary = "获取当前选中部门", description = "优先从缓存读取当前用户选中的部门，缓存未命中时使用用户所属部门并回填缓存。")
    @GetMapping("/current")
    public ApiResponse<DeptVO> current() {
        String userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.fail("用户未登录");
        }
        return ApiResponse.ok(service.getSelectedDept(userId));
    }

    /**
     * 获取当前用户选中部门的区域信息
     *
     * @return 当前选中部门关联区域
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "当前部门区域信息查询")
    @Operation(summary = "获取当前部门区域信息", description = "根据当前用户选中部门的区域编码查询关联行政区域信息。")
    @GetMapping("/current/region")
    public ApiResponse<RegionVO> currentRegion() {
        String userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.fail("用户未登录");
        }
        return ApiResponse.ok(service.getSelectedDeptRegion(userId));
    }

    /**
     * 查询部门树
     *
     * @return 部门树形结构
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '树查询'}")
    @Operation(summary = "查询部门树", description = "获取完整的部门树形结构，用于部门展示和管理。")
    @GetMapping("/tree")
    public ApiResponse<List<DeptVO>> tree() {
        return ApiResponse.ok(service.tree());
    }

}
