package com.ai.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Auth 侧超级管理员配置。
 * <p>
 * 超级管理员只由 Auth 配置中心判断，系统用户数据表不再保存超级管理员字段。
 */
@ConfigurationProperties(prefix = "ai.security.super-admin")
public record AuthSuperAdminProperties(
        boolean enabled,
        String username,
        String passwordHash
) {

    private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    public AuthSuperAdminProperties {
        if (enabled && (!StringUtils.hasText(username) || !StringUtils.hasText(passwordHash))) {
            throw new IllegalArgumentException("启用平台超级管理员时必须配置独立账号和密码哈希");
        }
        if (enabled && !BCRYPT_HASH.matcher(passwordHash.trim()).matches()) {
            throw new IllegalArgumentException("平台超级管理员密码必须配置为原始 BCrypt 哈希");
        }
        if (StringUtils.hasText(username)) {
            username = username.trim();
        }
        if (StringUtils.hasText(passwordHash)) {
            passwordHash = passwordHash.trim();
        }
    }

    public boolean configured() {
        return enabled && StringUtils.hasText(username) && StringUtils.hasText(passwordHash);
    }

    public boolean matchesUsername(String candidate) {
        return configured() && StringUtils.hasText(candidate) && username.equals(candidate.trim());
    }

    /**
     * 返回不包含超级管理员账号和密码哈希的诊断摘要。
     *
     * @return 已脱敏的超级管理员配置摘要
     */
    @Override
    public String toString() {
        return "AuthSuperAdminProperties[enabled=" + enabled + ", credentials=<redacted>]";
    }
}
