package com.ai.system.domain.dto;

import com.ai.api.security.PasswordDigestProtocol;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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

        @NotBlank(message = "旧密码摘要不能为空")
        @Pattern(regexp = PasswordDigestProtocol.DIGEST_REGEX, message = "旧密码摘要必须为40位小写SHA-1十六进制字符串")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(description = "旧密码的SHA-1小写十六进制摘要", accessMode = Schema.AccessMode.WRITE_ONLY)
        String oldPasswordDigest,

        @NotBlank(message = "新密码摘要不能为空")
        @Pattern(regexp = PasswordDigestProtocol.DIGEST_REGEX, message = "新密码摘要必须为40位小写SHA-1十六进制字符串")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(description = "新密码的SHA-1小写十六进制摘要", accessMode = Schema.AccessMode.WRITE_ONLY)
        String newPasswordDigest
) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[oldPasswordDigest=[REDACTED], newPasswordDigest=[REDACTED]]";
    }
}
