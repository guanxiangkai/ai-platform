package com.ai.gateway.constant;

/**
 * Gateway GlobalFilter 执行顺序常量
 * <p>
 * 数值越小越先执行。集中定义避免魔法数字散落在各 Filter 中。
 * </p>
 *
 * <pre>
 * ┌─────────────────────── Spring Security WebFilter 链 ──────────────────────┐
 * │  SecurityWebFilterChain → GatewayJwtAuthFilter（RSA 公钥本地验证 JWT）      │
 * └──────────────────────────────────────────────────────────────────────────┘
 *                                    ↓
 * ┌─────────────────────── Gateway GlobalFilter 链 ──────────────────────────┐
 * │  RequestLogFilter(-200)       请求日志                                     │
 * │  RateLimitFilter(-150)        限流 &amp; 黑名单                              │
 * │  SecurityFilter(-120)         XSS / SQL 注入检测                          │
 * │  TenantRouteGuard(-110)       校验业务路径租户与 JWT 租户一致               │
 * │  UserInfoForwardFilter(-100)  透传用户信息到下游（防伪造头清洗 + 写入身份头）  │
 * │  → 路由转发到下游服务                                                      │
 * └──────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class FilterOrder {

    /**
     * 请求日志
     */
    public static final int REQUEST_LOG = -200;
    /**
     * 限流 & IP 黑名单
     */
    public static final int RATE_LIMIT = -150;
    /**
     * XSS / SQL 注入安全检测
     */
    public static final int SECURITY = -120;
    /**
     * 租户业务路由校验。
     */
    public static final int TENANT_ROUTE_GUARD = -110;
    /**
     * 用户信息下游透传（从 SecurityContext 读取，写入请求头）
     */
    public static final int USER_INFO_FORWARD = -100;

    private FilterOrder() {
    }
}
