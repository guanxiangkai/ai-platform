package com.ai.auth.properties;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 配置属性 Record
 * <p>
 * Auth 服务使用 privateKey / keyId / expiration / issuer 签发 JWT，
 * 仅 Auth 服务需要此配置，下游服务和 Gateway 不依赖此类。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RegisterReflectionForBinding
@ConfigurationProperties(prefix = "ai.security.jwt")
public record JwtProperties(
        String privateKey,
        String keyId,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration,
        String issuer
) {

    /**
     * 紧凑型构造器 - 设置默认值
     */
    public JwtProperties {
        if (keyId == null || keyId.isBlank()) {
            keyId = "ai-default-key";
        }
        if (accessTokenExpiration == null) {
            accessTokenExpiration = Duration.ofHours(2);
        }
        if (refreshTokenExpiration == null) {
            refreshTokenExpiration = Duration.ofDays(7);
        }
        if (issuer == null) {
            issuer = "ai-platform";
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration.getSeconds();
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpiration.getSeconds();
    }
}
