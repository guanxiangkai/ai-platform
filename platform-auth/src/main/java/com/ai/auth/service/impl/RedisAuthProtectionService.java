package com.ai.auth.service.impl;

import com.ai.auth.properties.AuthProtectionProperties;
import com.ai.auth.service.AuthProtectionService;
import com.ai.auth.constants.AuthModuleConstants;
import io.github.guanxiangkai.web.plus.core.crypto.SecurityFingerprint;
import io.github.guanxiangkai.web.plus.core.exception.BaseException;
import io.github.guanxiangkai.web.plus.core.net.ClientIpResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Redis-backed 认证接口防刷实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisAuthProtectionService implements AuthProtectionService {

    private final StringRedisTemplate redisTemplate;
    private final AuthProtectionProperties properties;
    private final ClientIpResolver clientIpResolver;

    @Override
    public void assertLoginAllowed(String username, ServerHttpRequest request) {
        if (!properties.enabled()) {
            return;
        }
        String key = loginSubjectKey(username, request);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(AuthModuleConstants.RedisKeyConstants.LOGIN_LOCK_PREFIX + key))) {
            log.warn("[AuthProtection] 登录已锁定: subject={}", key);
            throw new BaseException.BusinessException(429, "登录失败次数过多，请稍后再试");
        }
    }

    @Override
    public void recordLoginSuccess(String username, ServerHttpRequest request) {
        if (!properties.enabled()) {
            return;
        }
        String key = loginSubjectKey(username, request);
        redisTemplate.delete(AuthModuleConstants.RedisKeyConstants.LOGIN_FAIL_PREFIX + key);
        redisTemplate.delete(AuthModuleConstants.RedisKeyConstants.LOGIN_LOCK_PREFIX + key);
    }

    @Override
    public void recordLoginFailure(String username, ServerHttpRequest request) {
        if (!properties.enabled()) {
            return;
        }
        String subject = loginSubjectKey(username, request);
        String failKey = AuthModuleConstants.RedisKeyConstants.LOGIN_FAIL_PREFIX + subject;
        Long failures = redisTemplate.opsForValue().increment(failKey);
        redisTemplate.expire(failKey, properties.loginFailureWindow());

        if (failures != null && failures >= properties.loginMaxFailures()) {
            redisTemplate.opsForValue().set(AuthModuleConstants.RedisKeyConstants.LOGIN_LOCK_PREFIX + subject, "1", properties.loginLockDuration());
            log.warn("[AuthProtection] 登录失败触发锁定: subject={}, failures={}, lock={}s",
                    subject, failures, properties.loginLockDuration().toSeconds());
        }
    }

    @Override
    public void assertRefreshAllowed(String refreshToken, ServerHttpRequest request) {
        if (!properties.enabled()) {
            return;
        }
        String key = refreshSubjectKey(refreshToken, request);
        Long attempts = redisTemplate.opsForValue().increment(AuthModuleConstants.RedisKeyConstants.REFRESH_RATE_PREFIX + key);
        redisTemplate.expire(AuthModuleConstants.RedisKeyConstants.REFRESH_RATE_PREFIX + key, properties.refreshWindow());
        if (attempts != null && attempts > properties.refreshMaxAttempts()) {
            log.warn("[AuthProtection] Refresh 触发限流: subject={}, attempts={}, window={}s",
                    key, attempts, properties.refreshWindow().toSeconds());
            throw new BaseException.BusinessException(429, "刷新令牌过于频繁，请稍后再试");
        }
    }

    @Override
    public void recordRefreshSuccess(String refreshToken, ServerHttpRequest request) {
        if (!properties.enabled()) {
            return;
        }
        redisTemplate.delete(AuthModuleConstants.RedisKeyConstants.REFRESH_RATE_PREFIX + refreshSubjectKey(refreshToken, request));
    }

    private String loginSubjectKey(String username, ServerHttpRequest request) {
        return SecurityFingerprint.sha256(normalize(username) + "\n"
                + clientIpResolver.resolve(request));
    }

    private String refreshSubjectKey(String refreshToken, ServerHttpRequest request) {
        return SecurityFingerprint.sha256(refreshToken + "\n"
                + clientIpResolver.resolve(request));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "anonymous";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

}
