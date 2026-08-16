package com.ai.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        @Schema(description = "用户名", example = "example-user")
        @NotBlank(message = "用户名不能为空")
        String username,

        @Schema(description = "密码", example = "example-password")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "密码不能为空")
        String password,

        @Schema(description = "验证码", example = "1234")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String captcha,

        @Schema(description = "验证码Key")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String captchaKey
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 返回不包含账号、密码、验证码或验证码会话键的诊断摘要。 */
    @Override
    public String toString() {
        return "LoginRequest[credentials=<redacted>]";
    }
}
