package com.ai.auth.service.impl;

import com.ai.auth.constants.AuthModuleConstants;
import com.ai.auth.properties.AuthProtectionProperties;
import io.github.guanxiangkai.web.plus.core.net.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisAuthProtectionServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void loginRateLimitKeyDoesNotExposeUsernameOrIp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenReturn(1L);
        RedisAuthProtectionService service = new RedisAuthProtectionService(
                redis, properties(), request -> "203.0.113.8");

        service.recordLoginFailure("Private.User", MockServerHttpRequest.post("/auth/login").build());

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(values).increment(key.capture());
        assertThat(key.getValue())
                .startsWith(AuthModuleConstants.RedisKeyConstants.LOGIN_FAIL_PREFIX)
                .doesNotContain("Private.User", "private.user", "203.0.113.8")
                .matches("security:auth:login:fail:[0-9a-f]{64}");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshRateLimitKeyDoesNotExposeTokenOrIp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenReturn(1L);
        ClientIpResolver resolver = request -> "203.0.113.8";
        RedisAuthProtectionService service = new RedisAuthProtectionService(redis, properties(), resolver);

        service.assertRefreshAllowed("refresh-token-secret", MockServerHttpRequest.post("/auth/refresh").build());

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(values).increment(key.capture());
        assertThat(key.getValue())
                .startsWith(AuthModuleConstants.RedisKeyConstants.REFRESH_RATE_PREFIX)
                .doesNotContain("refresh-token-secret", "203.0.113.8")
                .matches("security:auth:refresh:rate:[0-9a-f]{64}");
    }

    private static AuthProtectionProperties properties() {
        return new AuthProtectionProperties(true, 5, Duration.ofMinutes(15),
                Duration.ofMinutes(30), 20, Duration.ofMinutes(5));
    }
}
