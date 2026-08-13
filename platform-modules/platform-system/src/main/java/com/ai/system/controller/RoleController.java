package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresPermission;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.system.domain.dto.RoleDTO;
import com.ai.system.domain.dto.RolePageDTO;
import com.ai.system.domain.dto.RolePermissionAssignDTO;
import com.ai.system.domain.entity.Role;
import com.ai.system.domain.vo.MenuVO;
import com.ai.system.domain.vo.RolePageVO;
import com.ai.system.domain.vo.RoleVO;
import com.ai.system.service.IMenuService;
import com.ai.system.service.IRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "角色管理", description = "角色CRUD、权限分配等功能")
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class RoleController extends BaseController<RolePageDTO, RolePageVO, RoleVO, RoleDTO, RoleDTO, Role> {

    private final IRoleService service;
    private final IMenuService menuService;

    @Override
    protected IBaseService<RolePageDTO, RolePageVO, RoleVO, RoleDTO, RoleDTO, Role> getService() {
        return this.service;
    }

    /**
     * 获取角色选项列表
     *
     * @return 角色选项列表
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '选项列表'}")
    @Operation(summary = "获取角色选项列表", description = "获取所有角色的选项列表，用于下拉选择等场景。")
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.ok(service.options());
    }

    /**
     * 检查角色编码是否可用
     *
     * @param code 角色编码
     * @return 编码是否可用
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '编码检查'}")
    @Operation(summary = "检查角色编码", description = "检查角色编码是否已被使用。")
    @GetMapping("/check")
    public ApiResponse<Boolean> checkCode(@RequestParam String code) {
        return ApiResponse.ok(service.checkCode(code));
    }

    /**
     * 获取角色权限
     *
     * @param id 角色ID
     * @return 角色权限ID列表
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '权限查询'}")
    @Operation(summary = "获取角色权限", description = "查询角色已分配的权限列表。")
    @RequiresPermission("system:role:assignPermission")
    @GetMapping("/{id}/permissions")
    public ApiResponse<List<String>> getRolePermissions(@PathVariable String id) {
        return ApiResponse.ok(service.getRolePermissions(id));
    }

    /**
     * 分配角色权限
     *
     * @param id            角色ID
     * @param request 权限ID列表
     * @return 分配是否成功
     */
    @RequiresPermission("system:role:assignPermission")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '权限分配'}")
    @Operation(summary = "分配角色权限", description = "为角色分配权限，会覆盖原有权限配置。")
    @PutMapping("/{id}/permissions")
    public ApiResponse<Boolean> assignPermissions(@PathVariable String id, @Valid @RequestBody RolePermissionAssignDTO request) {
        return ApiResponse.ok(service.assignPermissions(id, request.permissionIds()));
    }

    /**
     * 复制角色
     *
     * @param id 角色ID
     * @return 新角色ID
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.INSERT, module = "#{getModuleName()}", description = "#{getEntityName() + '复制'}")
    @Operation(summary = "复制角色", description = "复制现有角色，包括权限配置。")
    @PostMapping("/{id}/duplicate")
    public ApiResponse<String> duplicate(@PathVariable String id) {
        return ApiResponse.ok(service.duplicate(id));
    }

    /**
     * 获取所有权限列表
     *
     * @return 权限列表
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '权限列表查询'}")
    @Operation(summary = "获取所有权限列表", description = "获取系统中所有可用的权限列表。")
    @RequiresPermission("system:role:assignPermission")
    @GetMapping("/permissions")
    public ApiResponse<List<MenuVO>> getPermissions() {
        return ApiResponse.ok(menuService.getAssignableMenus());
    }

    /**
     * 获取权限分组列表
     *
     * @return 权限分组列表
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '权限分组查询'}")
    @Operation(summary = "获取权限分组列表", description = "获取权限的分组列表，用于权限树展示。")
    @RequiresPermission("system:role:assignPermission")
    @GetMapping("/permissions/groups")
    public ApiResponse<List<MenuVO>> getPermissionGroups() {
        return ApiResponse.ok(menuService.getAssignableMenuTree());
    }


}
