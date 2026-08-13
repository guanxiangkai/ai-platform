package com.ai.api.security;

/**
 * 平台超级管理员在认证、网关验签和审计可见性中共用的稳定标识。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class PlatformSuperAdmin {

    /** 不与租户业务账户重合的平台超级管理员用户标识。 */
    public static final String USER_ID = "platform-super-admin";

    /** 认证服务写入、网关验证的超级管理员 Token 版本键。 */
    public static final String TOKEN_VERSION_CACHE_KEY = "platform:auth:super-admin:token-version";

    private PlatformSuperAdmin() {
    }
}
