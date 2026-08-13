package com.ai.api.security;

import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;

/**
 * 平台认证缓存键生成器。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class PlatformAuthCacheKeys {

    private PlatformAuthCacheKeys() {
    }

    /**
     * 生成租户内用户名索引键。
     *
     * @param tenantId 租户标识
     * @param username 用户名
     * @return 租户内唯一的用户名索引键
     */
    public static String usernameIndex(String tenantId, String username) {
        return AuthConstants.UserAuthCacheConstants.USERNAME_INDEX_PREFIX
                + requireText(tenantId, "tenantId")
                + ":"
                + requireText(username, "username");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
