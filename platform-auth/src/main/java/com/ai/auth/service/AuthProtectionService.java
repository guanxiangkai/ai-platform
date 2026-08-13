package com.ai.auth.service;

import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * 认证接口防刷服务。
 */
public interface AuthProtectionService {

    void assertLoginAllowed(String username, ServerHttpRequest request);

    void recordLoginSuccess(String username, ServerHttpRequest request);

    void recordLoginFailure(String username, ServerHttpRequest request);

    void assertRefreshAllowed(String refreshToken, ServerHttpRequest request);

    void recordRefreshSuccess(String refreshToken, ServerHttpRequest request);
}
