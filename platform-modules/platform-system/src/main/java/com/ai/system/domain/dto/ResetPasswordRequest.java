package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.security.password.PasswordProtocol;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 管理员重置密码请求。
 *
 * <p>管理端在本地生成独立随机原密码，计算 SHA-1 摘要后提交；服务端不会接收或回传原密码。</p>
 */
@Schema(description = "管理员重置密码请求")
public record ResetPasswordRequest(
        @NotBlank(message = "新密码摘要不能为空")
        @Pattern(regexp = PasswordProtocol.PASSWORD_PATTERN, message = "新密码摘要必须为40位小写SHA-1十六进制字符串")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(description = "新密码的SHA-1小写十六进制摘要", accessMode = Schema.AccessMode.WRITE_ONLY)
        String newPassword
) {
    @Override
    public String toString() {
        return "ResetPasswordRequest[newPassword=[REDACTED]]";
    }
}
