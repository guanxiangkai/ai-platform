package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 用户分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户分页查询参数")
public class UserPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索条件")
    private String name;

    @Schema(description = "邮箱（模糊查询）")
    private String email;

    @Schema(description = "手机号（模糊查询）")
    private String phone;

    @Schema(description = "性别", ref = "Gender")
    private Integer gender;

    @Schema(description = "启用状态(0:禁用,1:启用)")
    private Boolean enabled;

    @Schema(description = "用户类型(ADMIN-管理员,USER-用户)")
    private String userType;

    @Schema(description = "角色编码")
    private String roleCode;
}