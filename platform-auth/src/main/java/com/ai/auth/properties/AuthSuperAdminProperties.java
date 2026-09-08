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
        /** 是否启用平台超级管理员登录。 */
        boolean enabled,
        /** 平台超级管理员登录用户名。 */
        String username,
        /** 前端一次 SHA-1(UTF-8 原始密码)得到的40位小写摘要。 */
        String passwordDigest
) {

    public AuthSuperAdminProperties {
        if (StringUtils.hasText(username)) {
            username = username.trim();
        }
        if (StringUtils.hasText(passwordDigest)) {
            passwordDigest = passwordDigest.trim();
        }
        if (enabled && (!StringUtils.hasText(username) || !StringUtils.hasText(passwordDigest))) {
            throw new IllegalArgumentException("启用平台超级管理员时必须配置独立账号和密码摘要");
        }
        if (enabled) {
            try {
                PasswordProtocol.requirePassword(passwordDigest);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "平台超级管理员密码必须配置为非空原始密码的40位小写SHA-1摘要", exception);
            }
        }
    }

    /**
     * 判断超级管理员配置是否完整且启用。
     *
     * @return 配置完整并启用时返回 {@code true}
     */
    public boolean configured() {
        return enabled && StringUtils.hasText(username) && StringUtils.hasText(passwordDigest);
    }

    /**
     * 判断候选用户名是否为配置的固定超级管理员账号。
     *
     * @param candidate 待匹配用户名
     * @return 匹配时返回 {@code true}
     */
    public boolean matchesUsername(String candidate) {
        return configured() && StringUtils.hasText(candidate) && username.equals(candidate.trim());
    }

    /**
     * 返回不包含用户名和密码等效凭据的安全文本表示，避免配置对象被日志意外泄露。
     *
     * @return 仅包含启用状态的脱敏文本
     */
    @Override
    public String toString() {
        return "AuthSuperAdminProperties[enabled=" + enabled + ", username=[REDACTED], passwordDigest=[REDACTED]]";
    }
}
