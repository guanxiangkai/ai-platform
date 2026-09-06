package com.ai.auth.controller;

import io.github.guanxiangkai.web.plus.log.annotation.LoginLog;
import com.ai.auth.domain.dto.LoginRequest;
import com.ai.auth.domain.vo.LoginResponse;
import com.ai.auth.log.AuthLoginLogRecord;
import com.ai.auth.service.IAuthService;
import io.github.guanxiangkai.web.plus.core.exception.BaseException;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 认证控制器
 * <p>
 * 职责：用户登录（JWT 签发）、登出（Token 吊销）、Token 刷新。
 * <br/>
 * ⚠️ Token 验证由 Gateway 持有公钥本地完成，Auth 不提供验证接口。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "认证管理", description = "用户认证相关接口")
@RestController
@RequestMapping({"/auth", "/api/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "提交用户名和 UTF-8 原始密码的SHA-1小写十六进制摘要进行登录，返回访问令牌和刷新令牌")
    @PostMapping("/login")
    public Mono<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            ServerWebExchange exchange) {
        return authService.login(request, exchange)
                .map(ApiResponse::ok)
                .onErrorResume(BaseException.BusinessException.class,
                        error -> Mono.just(ApiResponse.fail(error.getCode(), error.getMessage())));
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出", description = "退出当前登录，清除Token")
    @LoginLog(entity = AuthLoginLogRecord.class, action = "LOGOUT")
    @PostMapping("/logout")
    public Mono<ApiResponse<Boolean>> logout(ServerWebExchange exchange) {
        return authService.logout(exchange).map(ApiResponse::ok);
    }

    /**
     * 刷新Token
     */
    @Operation(summary = "刷新Token", description = "使用刷新令牌获取新的访问令牌")
    @LoginLog(entity = AuthLoginLogRecord.class, action = "REFRESH_TOKEN")
    @PostMapping("/refresh")
    public Mono<ApiResponse<LoginResponse>> refreshToken(
            @Parameter(description = "刷新令牌") @RequestParam String refreshToken,
            ServerWebExchange exchange) {
        return authService.refreshToken(refreshToken, exchange).map(ApiResponse::ok);
    }
}
