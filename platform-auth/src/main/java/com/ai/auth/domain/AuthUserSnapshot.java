package com.ai.auth.domain;

import cn.hutool.json.JSONUtil;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

public record AuthUserSnapshot(
        String id,
        String username,
        String passwordHash,
        Boolean enabled,
        long tokenVersion,
        String nickname,
        String avatar,
        String userType,
        Boolean superAdmin,
        String tenantId,
        String deptId,
        Set<String> roleCodes,
        Set<String> postCodes,
        Set<String> permissions,
        Set<String> deptIds
) {

    public static AuthUserSnapshot fromHash(Map<Object, Object> hash) {
        if (hash == null || hash.isEmpty()) {
            return null;
        }
        String id = value(hash, AuthConstants.UserAuthCacheConstants.FIELD_ID);
        String username = value(hash, AuthConstants.UserAuthCacheConstants.FIELD_USERNAME);
        String passwordHash = value(hash, AuthConstants.UserAuthCacheConstants.FIELD_PASSWORD_HASH);
        if (!StringUtils.hasText(id) || !StringUtils.hasText(username) || !StringUtils.hasText(passwordHash)) {
            return null;
        }
        return new AuthUserSnapshot(
                id,
                username,
                passwordHash,
                Boolean.parseBoolean(value(hash, AuthConstants.UserAuthCacheConstants.FIELD_ENABLED)),
                longValue(hash, AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION),
                blankToNull(value(hash, AuthConstants.UserAuthCacheConstants.FIELD_NICKNAME)),
                blankToNull(value(hash, AuthConstants.UserAuthCacheConstants.FIELD_AVATAR)),
                blankToNull(value(hash, AuthConstants.UserAuthCacheConstants.FIELD_USER_TYPE)),
                false,
                blankToNull(value(hash, AuthConstants.UserAuthCacheConstants.FIELD_TENANT_ID)),
                blankToNull(value(hash, AuthConstants.UserAuthCacheConstants.FIELD_DEPT_ID)),
                stringSet(value(hash, AuthConstants.UserAuthCacheConstants.FIELD_ROLE_CODES)),
                stringSet(value(hash, AuthConstants.UserAuthCacheConstants.FIELD_POST_CODES)),
                stringSet(value(hash, AuthConstants.UserAuthCacheConstants.FIELD_PERMISSIONS)),
                stringSet(value(hash, AuthConstants.UserAuthCacheConstants.FIELD_DEPT_IDS))
        );
    }

    public AuthUserSnapshot withTokenVersion(long newTokenVersion) {
        return new AuthUserSnapshot(id, username, passwordHash, enabled, newTokenVersion, nickname, avatar,
                userType, superAdmin, tenantId, deptId, roleCodes, postCodes, permissions, deptIds);
    }

    public AuthUserSnapshot withSuperAdmin(boolean nextSuperAdmin) {
        return new AuthUserSnapshot(id, username, passwordHash, enabled, tokenVersion, nickname, avatar,
                userType, nextSuperAdmin, tenantId, deptId, roleCodes, postCodes, permissions, deptIds);
    }

    public boolean active() {
        return Boolean.TRUE.equals(enabled);
    }

    private static String value(Map<Object, Object> hash, String field) {
        Object value = hash.get(field);
        return value == null ? null : value.toString();
    }

    private static long longValue(Map<Object, Object> hash, String field) {
        String value = value(hash, field);
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    private static Set<String> stringSet(String json) {
        if (!StringUtils.hasText(json)) {
            return Set.of();
        }
        return Set.copyOf(JSONUtil.parseArray(json).toList(String.class));
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
