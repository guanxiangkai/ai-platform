package com.ai.gateway.config;

import com.ai.gateway.util.GatewayPathMatcher;
import com.ai.api.security.PlatformTokenProfile;
import io.github.guanxiangkai.web.plus.core.util.IpUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     * RSA 公钥 PEM 内容（从 Nacos 配置加载，用于本地验证 JWT 签名）
     */
    private String publicKey;

    /**
     * 公钥对应的受信任 JWT Key ID。
     */
    private String keyId = PlatformTokenProfile.DEFAULT_KEY_ID;

    /**
     * 受信任 JWT 签发方。
     */
    private String issuer = PlatformTokenProfile.ISSUER;

    /**
     * 网关接受的访问令牌受众。
     */
    private String accessTokenAudience = PlatformTokenProfile.ACCESS_TOKEN_AUDIENCE;

    /**
     * 分布式节点之间允许的最大时钟偏差。
     */
    private Duration allowedClockSkew = Duration.ofSeconds(60);

    /**
     * 设置网关唯一接受的 JWT Key ID。
     *
     * @param keyId 受信任公钥的 Key ID
     */
    public void setKeyId(String keyId) {
        this.keyId = requireText(keyId, "ai.gateway.key-id");
    }

    /**
     * 设置网关唯一接受的 JWT 签发方。
     *
     * @param issuer 受信任签发方
     */
    public void setIssuer(String issuer) {
        this.issuer = requireText(issuer, "ai.gateway.issuer");
    }

    /**
     * 设置网关唯一接受的访问令牌受众。
     *
     * @param accessTokenAudience 访问令牌受众
     */
    public void setAccessTokenAudience(String accessTokenAudience) {
        this.accessTokenAudience = requireText(accessTokenAudience, "ai.gateway.access-token-audience");
    }

    /**
     * 设置令牌时间声明允许的最大时钟偏差。
     *
     * @param allowedClockSkew 时钟偏差，范围为 0 到 5 分钟
     */
    public void setAllowedClockSkew(Duration allowedClockSkew) {
        if (allowedClockSkew == null || allowedClockSkew.isNegative()
                || allowedClockSkew.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("ai.gateway.allowed-clock-skew 必须在 0 到 5 分钟之间");
        }
        this.allowedClockSkew = allowedClockSkew;
    }

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
     * 产品前缀入口的匿名白名单，由部署环境按实际路由显式配置。
     *
     * <p>例如 {@code /product-a/auth/login}。基础项目不推断产品名，也不会自动把所有租户路径设为匿名。</p>
     */
    private List<String> tenantExcludePaths = List.of();

    /**
     * 判断路径是否属于平台或产品入口的匿名白名单。
     *
     * <p>JWT 过滤器与 Spring Security 授权链必须共同调用此方法，避免两处规则漂移。</p>
     *
     * @param path 请求路径
     * @return 命中任一匿名白名单时返回 {@code true}
     */
    public boolean isExcludedPath(String path) {
        return GatewayPathMatcher.matchesAny(excludePaths, path)
                || GatewayPathMatcher.matchesAny(tenantExcludePaths, path);
    }

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
     * 业务入口前缀与租户 ID 的映射，例如 {@code product -> <tenant-id>}。
     *
     * <p>该映射只在可信网关配置中维护，用于阻止租户 Token 访问不属于自身的业务路由。</p>
     */
    private Map<String, String> tenantPathIds = Map.of();

    /**
     * 网关入口前方可以提供可信转发头的精确代理 IP 列表。
     *
     * <p>默认空列表表示网关直接面向客户端，只使用 TCP 对端地址。若前方存在负载均衡或反向代理，
     * 必须按部署拓扑显式配置；不能把整个内网地址段视为可信代理。</p>
     */
    private List<String> trustedProxyIps = List.of();

    /**
     * 规范化可信代理 IP；非法主机名、CIDR 或其他非字面量配置会使配置绑定失败。
     *
     * @param trustedProxyIps 精确代理 IP 列表
     */
    public void setTrustedProxyIps(List<String> trustedProxyIps) {
        if (trustedProxyIps == null) {
            this.trustedProxyIps = List.of();
            return;
        }
        this.trustedProxyIps = trustedProxyIps.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .map(value -> {
                    String normalized = IpUtils.normalizeIpLiteral(value);
                    if (normalized == null) {
                        throw new IllegalArgumentException(
                                "ai.gateway.trusted-proxy-ips 仅支持合法 IP 字面量: " + value);
                    }
                    return normalized;
                })
                .distinct()
                .toList();
    }

    private static String requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(propertyName + " 不能为空");
        }
        return value.strip();
    }

    // ==================== 限流防刷 ====================

    /**
     * 是否启用限流
     */
    private boolean rateLimitEnabled = true;

    /**
     * Redis 限流后端不可用时是否继续放行请求。
     *
     * <p>默认 {@code false}，避免防暴力破解和全局限流在依赖故障时被静默绕过。
     * 只有部署方完成风险评估并具备其他入口防护时才可显式开启。</p>
     */
    private boolean rateLimitFailOpen;

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
