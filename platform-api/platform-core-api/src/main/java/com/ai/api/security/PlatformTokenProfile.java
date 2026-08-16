package com.ai.api.security;

/**
 * 平台认证服务和网关共同遵守的 JWT 协议概要。
 *
 * <p>访问令牌与刷新令牌使用互斥的 {@code typ}、{@code token_use} 和
 * {@code aud}，防止一个用途的令牌被替换到另一个验证上下文。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class PlatformTokenProfile {

    /** 平台令牌的默认签发方。 */
    public static final String ISSUER = "ai-platform";

    /** 单密钥基线的默认 Key ID；生产轮换时由部署配置同时覆盖 Auth 与 Gateway。 */
    public static final String DEFAULT_KEY_ID = "ai-default-key";

    /** 区分访问令牌和刷新令牌的 JWT 声明名。 */
    public static final String TOKEN_USE_CLAIM = "token_use";

    /** 访问令牌的 {@code token_use} 声明值。 */
    public static final String ACCESS_TOKEN_USE = "access";

    /** 刷新令牌的 {@code token_use} 声明值。 */
    public static final String REFRESH_TOKEN_USE = "refresh";

    /** 访问令牌的 JWT {@code typ} 头。 */
    public static final String ACCESS_TOKEN_TYPE = "ai-platform-access+jwt";

    /** 刷新令牌的 JWT {@code typ} 头。 */
    public static final String REFRESH_TOKEN_TYPE = "ai-platform-refresh+jwt";

    /** 访问令牌只允许提交给平台网关。 */
    public static final String ACCESS_TOKEN_AUDIENCE = "ai-platform-gateway";

    /** 刷新令牌只允许提交给平台认证服务。 */
    public static final String REFRESH_TOKEN_AUDIENCE = "ai-platform-auth";

    private PlatformTokenProfile() {
    }
}
