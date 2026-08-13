package com.ai.gateway.filter;

import com.ai.gateway.config.AiGatewayProperties;
import com.ai.gateway.constant.FilterOrder;
import com.ai.gateway.util.GatewayPathMatcher;
import com.ai.gateway.util.ReactiveResponseUtils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 校验租户业务路径与 JWT 租户是否一致。
 *
 * <p>由 {@link AiGatewayProperties#getTenantPathIds()} 配置的路径前缀对应不同租户。本过滤器在路由
 * 去除前缀之前读取原始路径，并拒绝跨租户 Token 访问，防止仅靠客户端导航形成伪隔离。</p>
 */
@Component
@RequiredArgsConstructor
public class TenantRouteGuardFilter implements GlobalFilter, Ordered {

    private final AiGatewayProperties properties;

    @Override
    public int getOrder() {
        return FilterOrder.TENANT_ROUTE_GUARD;
    }

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String prefix = firstPathSegment(path);
        String expectedTenantId = properties.getTenantPathIds().get(prefix);
        if (!StringUtils.hasText(expectedTenantId)) {
            return chain.filter(exchange);
        }
        if (GatewayPathMatcher.matchesAny(properties.getExcludePaths(), path)) {
            return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(context -> Mono.justOrEmpty(context.getAuthentication()))
                .filter(Authentication::isAuthenticated)
                // GatewayFilterChain 返回 Mono<Void>，正常完成时不发射元素。转换为有值 Mono，
                // 避免 switchIfEmpty 把“下游正常完成”误判为“没有认证信息”并二次写响应。
                .flatMap(authentication -> verifyTenant(exchange, chain, authentication, expectedTenantId)
                        .thenReturn(Boolean.TRUE))
                .switchIfEmpty(ReactiveResponseUtils.writeError(
                        exchange,
                        HttpStatus.UNAUTHORIZED,
                        "请先登录后访问租户业务"
                ).thenReturn(Boolean.FALSE))
                .then();
    }

    private Mono<Void> verifyTenant(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            Authentication authentication,
            String expectedTenantId
    ) {
        if (!(authentication.getDetails() instanceof Map<?, ?> claims)) {
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.FORBIDDEN, "Token 缺少租户信息");
        }
        if (Boolean.parseBoolean(String.valueOf(claims.get("superAdmin")))) {
            return chain.filter(exchange);
        }
        Object tenantClaim = claims.get("tenantId");
        String actualTenantId = tenantClaim == null ? "" : String.valueOf(tenantClaim);
        if (!expectedTenantId.equals(actualTenantId)) {
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.FORBIDDEN, "禁止跨租户访问业务服务");
        }
        return chain.filter(exchange);
    }

    private String firstPathSegment(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        int separator = normalized.indexOf('/');
        return separator < 0 ? normalized : normalized.substring(0, separator);
    }

}
