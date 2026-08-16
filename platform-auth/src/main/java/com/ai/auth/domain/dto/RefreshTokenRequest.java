package com.ai.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/**
 * 刷新令牌请求。
 *
 * <p>刷新令牌只通过 HTTPS 请求体传输，避免进入 URL、代理访问日志和浏览器历史。</p>
 *
 * @param refreshToken 当前刷新令牌
 */
@Schema(description = "刷新令牌请求")
public record RefreshTokenRequest(
        @Schema(description = "当前刷新令牌")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "刷新令牌不能为空")
        String refreshToken
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 返回不包含刷新令牌的诊断摘要。 */
    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=<redacted>]";
    }
}
