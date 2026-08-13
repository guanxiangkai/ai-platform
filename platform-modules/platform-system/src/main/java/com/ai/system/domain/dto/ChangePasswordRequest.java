package com.ai.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 修改密码请求 DTO
 * <p>
 * 使用请求体（非 QueryParam）传递密码，避免明文密码出现在访问日志和代理日志中。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "修改密码请求")
public record ChangePasswordRequest(

        @NotBlank(message = "旧密码不能为空")
        @Schema(description = "旧密码")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Schema(description = "新密码")
        String newPassword
) {
}
