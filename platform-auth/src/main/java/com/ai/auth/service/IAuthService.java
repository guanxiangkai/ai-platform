package com.ai.auth.service;

import com.ai.auth.domain.dto.LoginRequest;
import com.ai.auth.domain.vo.LoginResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 认证服务接口
 * <p>
 * 职责：Token 签发（登录）、刷新、吊销（登出）。
 * ⚠️ Token 验证已移至 Gateway 本地完成（JWKS 公钥），Auth 不再提供验证能力。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IAuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @param exchange 当前 HTTP 交换上下文
     * @return 登录响应（包含Token）
     */
    Mono<LoginResponse> login(LoginRequest request, ServerWebExchange exchange);

    /**
     * 用户登出
     *
     * @param exchange 当前 HTTP 交换上下文
     * @return 是否登出成功
     */
    Mono<Boolean> logout(ServerWebExchange exchange);

    /**
     * 刷新Token
     *
     * @param refreshToken 刷新令牌
     * @return 新的登录响应
     */
    Mono<LoginResponse> refreshToken(String refreshToken, ServerWebExchange exchange);
}
