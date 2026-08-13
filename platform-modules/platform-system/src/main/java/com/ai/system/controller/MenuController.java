package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import com.ai.system.domain.dto.MenuCreateDTO;
import com.ai.system.domain.dto.MenuDTO;
import com.ai.system.domain.dto.MenuPageDTO;
import com.ai.system.domain.entity.Menu;
import com.ai.system.domain.vo.MenuPageVO;
import com.ai.system.domain.vo.MenuVO;
import com.ai.system.service.IMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "菜单管理", description = "菜单CRUD、树形结构等功能")
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class MenuController extends BaseController<MenuPageDTO, MenuPageVO, MenuVO, MenuCreateDTO, MenuDTO, Menu> {

    private final IMenuService service;

    @Override
    protected IBaseService<MenuPageDTO, MenuPageVO, MenuVO, MenuCreateDTO, MenuDTO, Menu> getService() {
        return this.service;
    }

    /**
     * 查询菜单树
     *
     * @return 菜单树形结构
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '树查询'}")
    @Operation(summary = "查询菜单树", description = "获取完整的菜单树形结构，用于菜单展示和管理。")
    @GetMapping("/tree")
    public ApiResponse<List<MenuVO>> tree() {
        return ApiResponse.ok(service.tree());
    }

    /**
     * 查询用户菜单
     * <p>
     * 如果未提供 userId 参数，则获取当前登录用户的菜单
     * </p>
     *
     * @return 用户可访问的菜单列表
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '用户菜单查询'}")
    @Operation(summary = "查询用户菜单", description = "根据用户ID查询该用户有权限访问的菜单列表。如果不传userId则查询当前登录用户的菜单。")
    @GetMapping("/user")
    public ApiResponse<List<MenuVO>> getUserMenus() {

        return ApiResponse.ok(service.getUserMenus());
    }
}
