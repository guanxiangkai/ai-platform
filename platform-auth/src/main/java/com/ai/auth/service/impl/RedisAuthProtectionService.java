package com.ai.auth.service.impl;

import com.ai.auth.constants.AuthModuleConstants;
import com.ai.auth.properties.AuthProtectionProperties;
import com.ai.auth.service.AuthProtectionService;
import io.github.guanxiangkai.web.plus.core.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
        return normalize(username) + ":" + clientIp(request);
    }

    private String refreshSubjectKey(String refreshToken, ServerHttpRequest request) {
        return tokenHash(refreshToken) + ":" + clientIp(request);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "anonymous";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String clientIp(ServerHttpRequest request) {
        for (String header : AuthModuleConstants.HeaderConstants.CLIENT_IP_HEADERS) {
            String value = request.getHeaders().getFirst(header);
            String ip = firstHeaderValue(value);
            if (StringUtils.hasText(ip)) {
                return ip;
            }
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    private String firstHeaderValue(String value) {
        if (!StringUtils.hasText(value) || "unknown".equalsIgnoreCase(value)) {
            return null;
        }
        if (value.startsWith("for=")) {
            return value.substring(4).replace("\"", "");
        }
        int comma = value.indexOf(',');
        return comma >= 0 ? value.substring(0, comma).trim() : value.trim();
    }

    private String tokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((token == null ? "" : token).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest unavailable", e);
        }
    }
}
