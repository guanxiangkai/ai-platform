package com.ai.auth.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录请求
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "用户登录请求")
public record LoginRequest(
        @Schema(description = "用户名", example = "admin")
        @NotBlank(message = "用户名不能为空")
        String username,

        @Schema(description = "密码", example = "123456")
        @NotBlank(message = "密码不能为空")
        String password,

        @Schema(description = "验证码", example = "1234")
        String captcha,

        @Schema(description = "验证码Key")
        String captchaKey
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
