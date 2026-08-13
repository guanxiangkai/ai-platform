package com.ai.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 登录和 refresh 防刷配置。
 */
@ConfigurationProperties(prefix = "ai.security.auth-protection")
public record AuthProtectionProperties(
        boolean enabled,
        int loginMaxFailures,
        Duration loginFailureWindow,
        Duration loginLockDuration,
        int refreshMaxAttempts,
        Duration refreshWindow
) {

    public AuthProtectionProperties {
        if (loginMaxFailures <= 0) {
            loginMaxFailures = 5;
        }
        if (loginFailureWindow == null || loginFailureWindow.isNegative() || loginFailureWindow.isZero()) {
            loginFailureWindow = Duration.ofMinutes(15);
        }
        if (loginLockDuration == null || loginLockDuration.isNegative() || loginLockDuration.isZero()) {
            loginLockDuration = Duration.ofMinutes(30);
        }
        if (refreshMaxAttempts <= 0) {
            refreshMaxAttempts = 20;
        }
        if (refreshWindow == null || refreshWindow.isNegative() || refreshWindow.isZero()) {
            refreshWindow = Duration.ofMinutes(5);
        }
    }

    public AuthProtectionProperties() {
        this(true, 5, null, null, 20, null);
    }
}
