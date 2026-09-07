package com.ai.auth.properties;

import io.github.guanxiangkai.web.plus.security.password.PasswordProtocol;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;


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

    public AuthSuperAdminProperties {
        if (enabled && (!StringUtils.hasText(username) || !StringUtils.hasText(passwordHash))) {
            throw new IllegalArgumentException("启用平台超级管理员时必须配置独立账号和密码哈希");
        }
        if (enabled && !PasswordProtocol.isBcryptHash(passwordHash.trim())) {
            throw new IllegalArgumentException("平台超级管理员密码必须配置为标准裸 BCrypt 哈希");
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
}
