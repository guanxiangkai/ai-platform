package com.ai.auth.domain.dto;

import io.github.guanxiangkai.web.plus.security.password.PasswordProtocol;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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

        @Schema(description = "UTF-8原始密码的SHA-1小写十六进制摘要", example = "40位小写hex", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "密码摘要不能为空")
        @Pattern(regexp = PasswordProtocol.PASSWORD_PATTERN, message = "密码摘要必须为40位小写SHA-1十六进制字符串")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password,

        @Schema(description = "验证码", example = "1234")
        String captcha,

        @Schema(description = "验证码Key")
        String captchaKey
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=[REDACTED], captcha=[REDACTED], captchaKey=[REDACTED]]";
    }
}
