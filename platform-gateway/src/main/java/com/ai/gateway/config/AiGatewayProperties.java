package com.ai.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 网关自定义配置属性
 * <p>
 * 包含网关特有的配置：JWT 公钥验证、限流、安全防护、下游透传请求头。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "ai.gateway")
public class AiGatewayProperties {

    // ==================== JWT 公钥验证 ====================

    /**
     * RSA 公钥 PEM 内容（由受控运行环境注入，用于本地验证 JWT 签名）
     */
    private String publicKey;

    /**
     * JWT 白名单路径（不需要认证的路径）
     */
    private List<String> excludePaths = List.of(
            "/auth/login",
            "/auth/refresh",
            "/api/auth/login",
            "/api/auth/refresh",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/webjars/**",
            "/favicon.ico",
            "/error",
            "/actuator/health",
            "/actuator/info"
    );

    /**
     * HTTP 请求解码配置。
     */
    private Netty netty = new Netty();

    // ==================== 下游透传请求头 ====================

    /**
     * 传递给下游服务的用户 ID 请求头
     */
    private String userIdHeader = "X-User-Id";
    /**
     * 传递给下游服务的用户 Claims 请求头
     */
    private String userClaimsHeader = "X-User-Claims";
    /**
     * 传递给下游服务的租户 ID 请求头
     */
    private String tenantIdHeader = "X-Tenant-Id";

    /**
     * 租户入口前缀与租户 ID 的映射，例如 {@code alpha -> tenant-alpha}。
     *
     * <p>该映射只在可信运行环境中维护，用于阻止 Token 跨租户访问服务路由。</p>
     */
    private Map<String, String> tenantPathIds = Map.of();

    // ==================== 限流防刷 ====================

    /**
     * 是否启用限流
     */
    private boolean rateLimitEnabled = true;

    /**
     * IP 维度 — 每个时间窗口内最大请求数
     */
    private int ipMaxRequests = 200;
    /**
     * IP 维度 — 时间窗口（秒）
     */
    private int ipWindowSeconds = 60;

    /**
     * 用户维度 — 每个时间窗口内最大请求数
     */
    private int userMaxRequests = 100;
    /**
     * 用户维度 — 时间窗口（秒）
     */
    private int userWindowSeconds = 60;

    /**
     * 登录接口 — 单 IP 每个时间窗口最大尝试次数（防暴力破解）
     */
    private int loginMaxAttempts = 10;
    /**
     * 登录接口 — 时间窗口（秒）
     */
    private int loginWindowSeconds = 300;

    /**
     * 限流 Redis Key 前缀
     */
    private String rateLimitKeyPrefix = "gateway:rate:";

    /**
     * IP 黑名单（被封禁的 IP）
     */
    private List<String> ipBlacklist = List.of();

    // ==================== 安全防护 ====================

    /**
     * 是否启用 XSS 防护
     */
    private boolean xssEnabled = true;

    /**
     * XSS 防护跳过的路径
     */
    private List<String> xssExcludePaths = List.of(
            "/actuator/**"
    );

    @Data
    public static class Netty {

        /**
         * 请求行最大长度。登录后前端可能携带较长查询参数，默认 4096 容易在解码阶段被拒绝。
         */
        private int maxInitialLineLength = 32 * 1024;

        /**
         * 请求头最大长度。默认 8192，JWT、Trace、浏览器 Cookie 叠加后可能超过该限制。
         */
        private int maxHeaderSize = 64 * 1024;
    }
}
