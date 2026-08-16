package com.ai.gateway.filter;

import cn.hutool.json.JSONUtil;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import io.github.guanxiangkai.web.plus.core.util.UserClaimsCodec;
import com.ai.gateway.util.ReactiveRequestUtils;
import com.ai.gateway.config.AiGatewayProperties;
import com.ai.gateway.constant.FilterOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 用户信息下游透传过滤器
 * <p>
 * 职责单一：从 {@link ReactiveSecurityContextHolder} 中读取已认证的用户信息，
 * 写入请求头（{@code X-User-Id} / {@code X-Tenant-Id} / {@code X-User-Claims}）透传给下游微服务。
 * {@code X-User-Claims} 使用 UTF-8 Base64URL 编码，避免中文昵称等非 ASCII 字符在请求头中损坏。
 * <br/>
 * <b>⚠️ 不做任何 JWT 校验！</b>认证由网关 {@code GatewayJwtAuthFilter} 统一处理。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserInfoForwardFilter implements GlobalFilter, Ordered {

    /**
     * JWT 内部字段，不透传给下游
     */
    private static final Set<String> JWT_INTERNAL_KEYS = Set.of("sub", "iss", "iat", "exp");
    private static final Set<String> AUTHORIZATION_SCOPE_KEYS = Set.of("permissions", "roles", "posts", "deptIds");
    private final AiGatewayProperties props;
    private final TrustedForwardProperties trustedForwardProperties;

    @Override
    public int getOrder() {
        return FilterOrder.USER_INFO_FORWARD;
    }

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // ⚠️ 防伪造：先清除外部请求中可能伪造的身份头
        ServerHttpRequest cleaned = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(props.getUserIdHeader());
                    h.remove(props.getTenantIdHeader());
                    h.remove(props.getUserClaimsHeader());
                    h.remove(AuthConstants.HeaderConstants.USER_CLAIMS_ENCODING);
                    h.remove(AuthConstants.HeaderConstants.VERIFIED_CLIENT_IP);
                    h.remove(trustedForwardProperties.getHeaderName());
                })
                .build();
        ServerWebExchange cleanedExchange = exchange.mutate().request(cleaned).build();

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> Mono.justOrEmpty(ctx.getAuthentication()))
                .filter(Authentication::isAuthenticated)
                .flatMap(auth -> {
                    String userId = auth.getName();
                    Map<String, Object> claims = extractClaims(auth);
                    String tenantId = resolveTenantId(exchange, claims);

                    // 业务 claims（去掉 JWT 内部字段）
                    Map<String, Object> safeClaims = new HashMap<>(claims);
                    safeClaims.keySet().removeAll(JWT_INTERNAL_KEYS);
                    safeClaims.keySet().removeAll(AUTHORIZATION_SCOPE_KEYS);
                    safeClaims.computeIfAbsent("nickname", ignored -> safeClaims.get("username"));
                    safeClaims.remove("username");
                    safeClaims.put("userId", userId);
                    safeClaims.put("tenantId", tenantId);

                    String claimsJson = JSONUtil.toJsonStr(safeClaims);
                    String encodedClaims = UserClaimsCodec.encode(claimsJson);
                    String clientIp = ReactiveRequestUtils.getClientIp(
                            exchange.getRequest(), props.getTrustedProxyIps());

                    ServerHttpRequest mutated = cleanedExchange.getRequest().mutate()
                            .headers(h -> {
                                if (shouldStripAuthorization(exchange)) {
                                    h.remove(HttpHeaders.AUTHORIZATION);
                                }
                            })
                            .header(props.getUserIdHeader(), userId)
                            .header(props.getTenantIdHeader(), tenantId)
                            .header(props.getUserClaimsHeader(), encodedClaims)
                            .header(AuthConstants.HeaderConstants.USER_CLAIMS_ENCODING, UserClaimsCodec.BASE64URL_ENCODING)
                            .header(AuthConstants.HeaderConstants.VERIFIED_CLIENT_IP, clientIp)
                            .header(trustedForwardProperties.getHeaderName(), trustedForwardProperties.getToken())
                            .build();

                    return chain.filter(cleanedExchange.mutate().request(mutated).build())
                            .thenReturn(Boolean.TRUE);
                })
                // 未认证请求（白名单路径等）直接放行（已清洗伪造头）
                .switchIfEmpty(Mono.defer(() -> forwardAnonymousTenantContext(cleanedExchange, chain)
                        .thenReturn(Boolean.FALSE)))
                .then();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractClaims(Authentication auth) {
        if (auth.getDetails() instanceof Map<?, ?> details) {
            return (Map<String, Object>) details;
        }
        return Map.of();
    }

    private boolean shouldStripAuthorization(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        return !isAuthServicePath(path) && !isAgentServicePath(path);
    }

    private String resolveTenantId(ServerWebExchange exchange, Map<String, Object> claims) {
        Object tenantClaim = claims.get("tenantId");
        String tenantId = tenantClaim == null ? "" : String.valueOf(tenantClaim);
        if (StringUtils.hasText(tenantId)) {
            return tenantId;
        }
        String path = exchange.getRequest().getURI().getPath();
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        int separator = normalized.indexOf('/');
        String prefix = separator < 0 ? normalized : normalized.substring(0, separator);
        return props.getTenantPathIds().getOrDefault(prefix, "");
    }

    private boolean isAuthServicePath(String path) {
        return path.equals("/auth") || path.startsWith("/auth/")
                || path.equals("/api/auth") || path.startsWith("/api/auth/");
    }

    private boolean isAgentServicePath(String path) {
        return path.equals("/agent") || path.startsWith("/agent/")
                || path.equals("/api/agent") || path.startsWith("/api/agent/")
                || isTenantServicePath(path, "agent");
    }

    private boolean isTenantServicePath(String path, String service) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        int separator = normalized.indexOf('/');
        if (separator < 0 || !props.getTenantPathIds().containsKey(normalized.substring(0, separator))) {
            return false;
        }
        String remainder = normalized.substring(separator + 1);
        if (remainder.startsWith("api/")) {
            remainder = remainder.substring("api/".length());
        }
        return remainder.equals(service) || remainder.startsWith(service + "/");
    }

    private Mono<Void> forwardAnonymousTenantContext(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        int separator = normalized.indexOf('/');
        String prefix = separator < 0 ? normalized : normalized.substring(0, separator);
        String tenantId = props.getTenantPathIds().get(prefix);
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header(AuthConstants.HeaderConstants.VERIFIED_CLIENT_IP,
                        ReactiveRequestUtils.getClientIp(exchange.getRequest(), props.getTrustedProxyIps()))
                .header(trustedForwardProperties.getHeaderName(), trustedForwardProperties.getToken());
        if (org.springframework.util.StringUtils.hasText(tenantId)) {
            requestBuilder.header(props.getTenantIdHeader(), tenantId);
        }
        ServerHttpRequest request = requestBuilder.build();
        return chain.filter(exchange.mutate().request(request).build());
    }

}
