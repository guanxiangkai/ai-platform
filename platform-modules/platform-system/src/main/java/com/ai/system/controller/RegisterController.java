package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresPermission;
import com.ai.system.domain.dto.RegisterCreateDTO;
import com.ai.system.domain.dto.RegisterDTO;
import com.ai.system.domain.dto.RegisterPageDTO;
import com.ai.system.domain.entity.Register;
import com.ai.system.domain.vo.RegisterPageVO;
import com.ai.system.domain.vo.RegisterResultVO;
import com.ai.system.domain.vo.RegisterVO;
import com.ai.system.domain.vo.RegisterAuditResultVO;
import com.ai.system.service.IRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 注册记录管理控制器
 * <p>
 * 提供用户自助注册和管理员审核两个核心接口。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "注册记录管理", description = "用户自助注册申请提交及管理员审核功能")
@RestController
@RequestMapping("/system/register")
@RequiredArgsConstructor
public class RegisterController extends BaseController<RegisterPageDTO, RegisterPageVO, RegisterVO, RegisterCreateDTO, RegisterDTO, Register> {

    private final IRegisterService service;

    @Override
    protected IBaseService<RegisterPageDTO, RegisterPageVO, RegisterVO, RegisterCreateDTO, RegisterDTO, Register> getService() {
        return this.service;
    }


    /**
     * 用户自助注册
     * <p>
     * 无需登录即可调用。系统自动匹配员工档案，匹配失败则注册被拒绝。
     * 注册成功后返回系统生成的账号名，请提示用户务必记住该账号名。
     * </p>
     */
    @Operation(summary = "用户注册申请",
            description = "提交注册申请。password 必须为UTF-8原始密码的SHA-1小写十六进制摘要；系统将自动匹配员工档案并生成账号名。无需登录。")
    @PostMapping("/")
    public ApiResponse<RegisterResultVO> register(@Valid @RequestBody RegisterCreateDTO dto) {
        return ApiResponse.ok(service.register(dto));
    }

    /**
     * 管理员审核注册申请
     * <p>
     * 审核通过时自动创建用户账户并绑定员工档案；审核拒绝时仅更新状态。
     * </p>
     */
    @RequiresPermission("system:register:audit")
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "#{getEntityName() + '注册审核'}")
    @Operation(summary = "审核注册申请", description = "管理员审核注册申请，通过时自动创建用户账户。")
    @PostMapping("/{id}/audit")
    public ApiResponse<RegisterAuditResultVO> audit(
            @Parameter(description = "注册记录ID") @PathVariable String id,
            @Parameter(description = "是否通过") @RequestParam Boolean approved,
            @Parameter(description = "审核备注") @RequestParam(required = false) String auditRemark) {
        return ApiResponse.ok(service.audit(id, approved, auditRemark));
    }
}
