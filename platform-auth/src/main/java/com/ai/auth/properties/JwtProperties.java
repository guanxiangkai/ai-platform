package com.ai.auth.properties;

import com.ai.api.security.PlatformTokenProfile;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 签发和校验配置。
 * <p>
 * Auth 服务使用 privateKey / keyId / expiration / issuer 签发 JWT，
 * 仅 Auth 服务需要此配置，下游服务和 Gateway 不依赖此类。
 * </p>
 *
 * @param privateKey             Auth 服务私有的 PKCS#8 RSA 私钥 PEM 内容
 * @param keyId                 当前签名密钥的稳定标识
 * @param accessTokenExpiration 访问令牌有效期
 * @param refreshTokenExpiration 刷新令牌有效期
 * @param issuer                平台令牌签发方
 * @param accessTokenAudience   访问令牌唯一受众
 * @param refreshTokenAudience  刷新令牌唯一受众
 * @param allowedClockSkew      验证分布式时间声明时允许的最大时钟偏差
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
        String issuer,
        String accessTokenAudience,
        String refreshTokenAudience,
        Duration allowedClockSkew
) {

    /**
     * 规范化公开标识和有效期，并拒绝无效的安全边界配置。
     *
     * @param privateKey             Auth 服务私有的 PKCS#8 RSA 私钥 PEM 内容
     * @param keyId                 当前签名密钥的稳定标识
     * @param accessTokenExpiration 访问令牌有效期
     * @param refreshTokenExpiration 刷新令牌有效期
     * @param issuer                平台令牌签发方
     * @param accessTokenAudience   访问令牌唯一受众
     * @param refreshTokenAudience  刷新令牌唯一受众
     * @param allowedClockSkew      验证分布式时间声明时允许的最大时钟偏差
     */
    public JwtProperties {
        keyId = defaultIfBlank(keyId, PlatformTokenProfile.DEFAULT_KEY_ID);
        if (accessTokenExpiration == null) {
            accessTokenExpiration = Duration.ofHours(2);
        }
        if (accessTokenExpiration.isZero() || accessTokenExpiration.isNegative()) {
            throw new IllegalArgumentException("ai.security.jwt.access-token-expiration 必须为正数");
        }
        if (refreshTokenExpiration == null) {
            refreshTokenExpiration = Duration.ofDays(7);
        }
        if (refreshTokenExpiration.isZero() || refreshTokenExpiration.isNegative()) {
            throw new IllegalArgumentException("ai.security.jwt.refresh-token-expiration 必须为正数");
        }
        issuer = defaultIfBlank(issuer, PlatformTokenProfile.ISSUER);
        accessTokenAudience = defaultIfBlank(
                accessTokenAudience, PlatformTokenProfile.ACCESS_TOKEN_AUDIENCE);
        refreshTokenAudience = defaultIfBlank(
                refreshTokenAudience, PlatformTokenProfile.REFRESH_TOKEN_AUDIENCE);
        if (allowedClockSkew == null) {
            allowedClockSkew = Duration.ofSeconds(60);
        }
        if (allowedClockSkew.isNegative() || allowedClockSkew.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("ai.security.jwt.allowed-clock-skew 必须在 0 到 5 分钟之间");
        }
    }

    /**
     * 返回访问令牌有效期秒数。
     *
     * @return 访问令牌有效期秒数
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration.getSeconds();
    }

    /**
     * 返回刷新令牌有效期秒数。
     *
     * @return 刷新令牌有效期秒数
     */
    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpiration.getSeconds();
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.strip();
    }

    /**
     * 返回不包含 JWT 私钥的配置摘要。
     *
     * @return 已脱敏的 JWT 配置摘要
     */
    @Override
    public String toString() {
        return "JwtProperties[privateKey=<redacted>, keyId=" + keyId
                + ", accessTokenExpiration=" + accessTokenExpiration
                + ", refreshTokenExpiration=" + refreshTokenExpiration
                + ", issuer=" + issuer
                + ", accessTokenAudience=" + accessTokenAudience
                + ", refreshTokenAudience=" + refreshTokenAudience
                + ", allowedClockSkew=" + allowedClockSkew + ']';
    }
}
