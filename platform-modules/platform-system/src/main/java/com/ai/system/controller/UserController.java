package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresPermission;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.system.domain.dto.*;
import com.ai.system.domain.entity.User;
import com.ai.system.domain.vo.UserPageVO;
import com.ai.system.domain.vo.UserVO;
import com.ai.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 用户管理控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "用户管理", description = "用户CRUD、角色分配、密码管理等功能")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class UserController extends BaseController<UserPageDTO, UserPageVO, UserVO, UserCreateDTO, UserDTO, User> {

    private final IUserService service;

    @Override
    protected IBaseService<UserPageDTO, UserPageVO, UserVO, UserCreateDTO, UserDTO, User> getService() {
        return this.service;
    }


    /**
     * 修改密码（仅限修改当前登录用户自己的密码）
     *
     * @param request 包含旧密码和新密码的请求体（使用 RequestBody 而非 RequestParam，防止密码出现在访问日志中）
     * @return 修改是否成功
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '密码修改'}")
    @Operation(summary = "修改密码", description = "用户修改自己的密码，需要验证旧密码。")
    @PostMapping("/changePassword")
    public ApiResponse<Boolean> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String currentUserId = SecurityUtils.getUserId();
        if (currentUserId == null) {
            return ApiResponse.fail("用户未登录");
        }
        return ApiResponse.ok(service.changePassword(currentUserId, request.oldPassword(), request.newPassword()));
    }

    /**
     * 重置密码
     *
     * @param id 用户ID
     * @return 新密码
     */
    @RequiresPermission("system:user:resetPwd")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '密码重置'}")
    @Operation(summary = "重置密码", description = "管理员重置用户密码为默认密码。")
    @PostMapping("/resetPassword")
    public ApiResponse<String> resetPassword(@RequestParam String id) {
        return ApiResponse.ok(service.resetPassword(id));
    }

    /**
     * 检查用户名是否可用
     *
     * @param username 用户名
     * @return 用户名是否可用
     */
    @Operation(summary = "检查用户名", description = "检查用户名是否已被使用。")
    @GetMapping("/check")
    public ApiResponse<Boolean> checkUsername(@RequestParam String username) {
        return ApiResponse.ok(service.checkUsername(username));
    }

    /**
     * 获取用户角色
     *
     * @param id 用户ID
     * @return 用户角色ID列表
     */
    @Operation(summary = "获取用户角色", description = "查询用户已分配的角色列表。")
    @GetMapping("/{id}/roles")
    public ApiResponse<List<String>> getUserRoles(@PathVariable String id) {
        return ApiResponse.ok(service.getUserRoles(id));
    }

    /**
     * 分配用户角色
     *
     * @param id      用户ID
     * @param request 角色ID列表和用户类型
     * @return 分配是否成功
     */
    @RequiresPermission("system:user:assignRole")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '角色分配'}")
    @Operation(summary = "分配用户角色", description = "为用户分配角色，会覆盖原有角色配置。")
    @PutMapping("/{id}/roles")
    public ApiResponse<Boolean> assignRoles(@PathVariable String id, @Valid @RequestBody UserRoleAssignDTO request) {
        return ApiResponse.ok(service.assignRoles(id, request.roleIds(), request.userType()));
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * 返回当前登录用户的完整信息，包括用户基本信息、角色、部门等
     * </p>
     *
     * @return 当前登录用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的完整信息")
    @GetMapping("/profile")
    public ApiResponse<UserVO> getCurrentUser() {
        String userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.fail("用户未登录");
        }
        return ApiResponse.ok(service.detail(userId));
    }

}
