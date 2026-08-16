package com.ai.gateway.filter;

import cn.hutool.json.JSONUtil;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import com.ai.gateway.config.AiGatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class UserInfoForwardFilterTest {

    @Test
    void shouldForwardTrustedTenantContextForAnonymousPortalLogin() {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setTenantPathIds(Map.of("product-a", "tenant-1"));
        UserInfoForwardFilter filter = new UserInfoForwardFilter(properties, trustedForwardProperties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/product-a/auth/login")
                        .header("X-Tenant-Id", "spoofed-tenant")
                        .header("X-Trusted-Forward-Token", "spoofed-token")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            forwarded.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Tenant-Id"))
                .isEqualTo("tenant-1");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Trusted-Forward-Token"))
                .isEqualTo("trusted-token");
        assertThat(forwarded.get().getRequest().getHeaders()
                .getFirst(AuthConstants.HeaderConstants.VERIFIED_CLIENT_IP)).isEqualTo("unknown");
    }

    @Test
    void shouldOverwriteSpoofedVerifiedClientIpFromTrustedProxyChain() {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setTenantPathIds(Map.of("product-a", "tenant-1"));
        properties.setTrustedProxyIps(List.of("198.51.100.7"));
        UserInfoForwardFilter filter = new UserInfoForwardFilter(properties, trustedForwardProperties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/product-a/auth/login")
                        .header(AuthConstants.HeaderConstants.VERIFIED_CLIENT_IP, "192.0.2.99")
                        .header("X-Forwarded-For", "203.0.113.40")
                        .remoteAddress(new java.net.InetSocketAddress("198.51.100.7", 8080))
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            forwarded.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders()
                .getFirst(AuthConstants.HeaderConstants.VERIFIED_CLIENT_IP)).isEqualTo("203.0.113.40");
    }

    @Test
    void shouldForwardTrustedTenantContextForAnonymousApiCryptoConfig() {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setTenantPathIds(Map.of("product-b", "tenant-2"));
        UserInfoForwardFilter filter = new UserInfoForwardFilter(properties, trustedForwardProperties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/product-b/api/web-plus/api-crypto/config")
                        .header("X-Tenant-Id", "spoofed-tenant")
                        .header("X-Trusted-Forward-Token", "spoofed-token")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            forwarded.set(chainExchange);
            return Mono.empty();
        }).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-Tenant-Id")).isEqualTo("tenant-2");
        assertThat(headers.getFirst("X-Trusted-Forward-Token")).isEqualTo("trusted-token");
    }

    @Test
    void shouldStripAuthorizationAndAuthorizationScopeClaimsForDownstreamServices() {
        UserInfoForwardFilter filter = new UserInfoForwardFilter(new AiGatewayProperties(), trustedForwardProperties());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin-id", null, List.of());
        authentication.setDetails(Map.of(
                "nickname", "管理员",
                "tenantId", "tenant-1",
                "superAdmin", true,
                "permissions", Set.of("system:role:delete", "system:user:list"),
                "roles", Set.of("admin"),
                "deptIds", Set.of("dept-1", "dept-2")
        ));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/system/dept/tree")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer very-long-token")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        AtomicInteger invocationCount = new AtomicInteger();

        filter.filter(exchange, chainExchange -> {
                    invocationCount.incrementAndGet();
                    forwarded.set(chainExchange);
                    return Mono.empty();
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(invocationCount).hasValue(1);
        assertThat(headers.containsHeader(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("admin-id");
        assertThat(headers.getFirst("X-Tenant-Id")).isEqualTo("tenant-1");
        assertThat(headers.getFirst(AuthConstants.HeaderConstants.USER_CLAIMS_ENCODING)).isEqualTo("base64url");
        String claimsJson = new String(Base64.getUrlDecoder().decode(headers.getFirst("X-User-Claims")),
                StandardCharsets.UTF_8);
        assertThat(claimsJson)
                .contains("\"userId\":\"admin-id\"")
                .contains("\"nickname\":\"管理员\"")
                .contains("\"superAdmin\":true")
                .doesNotContain("username")
                .doesNotContain("permissions")
                .doesNotContain("roles")
                .doesNotContain("deptIds");
        assertThat(JSONUtil.parseObj(claimsJson).getStr("nickname")).isEqualTo("管理员");
    }

    @Test
    void shouldResolveSuperAdminTenantFromProductPath() {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setTenantPathIds(Map.of("product-a", "tenant-1", "product-b", "tenant-2"));
        UserInfoForwardFilter filter = new UserInfoForwardFilter(properties, trustedForwardProperties());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("platform-super-admin", null, List.of());
        authentication.setDetails(Map.of("superAdmin", true, "nickname", "超级管理员"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/product-a/api/system/log/login/list").build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
                    forwarded.set(chainExchange);
                    return Mono.empty();
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-Tenant-Id")).isEqualTo("tenant-1");
        String claimsJson = new String(Base64.getUrlDecoder().decode(headers.getFirst("X-User-Claims")),
                StandardCharsets.UTF_8);
        assertThat(JSONUtil.parseObj(claimsJson).getStr("tenantId")).isEqualTo("tenant-1");
    }

    @Test
    void shouldKeepAuthorizationForAuthServiceEndpoints() {
        UserInfoForwardFilter filter = new UserInfoForwardFilter(new AiGatewayProperties(), trustedForwardProperties());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin-id", null, List.of());
        authentication.setDetails(Map.of("tenantId", "tenant-1", "superAdmin", true));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
                    forwarded.set(chainExchange);
                    return Mono.empty();
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer token");
    }

    @Test
    void shouldKeepAuthorizationForApiAuthServiceEndpoints() {
        UserInfoForwardFilter filter = new UserInfoForwardFilter(new AiGatewayProperties(), trustedForwardProperties());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin-id", null, List.of());
        authentication.setDetails(Map.of("tenantId", "tenant-1", "superAdmin", true));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
                    forwarded.set(chainExchange);
                    return Mono.empty();
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer token");
    }

    @Test
    void shouldKeepAuthorizationForAgentServiceEndpoints() {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setTenantPathIds(Map.of("product-a", "tenant-1", "product-b", "tenant-2"));
        UserInfoForwardFilter filter = new UserInfoForwardFilter(properties, trustedForwardProperties());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin-id", null, List.of());
        authentication.setDetails(Map.of("tenantId", "tenant-1", "superAdmin", true));

        assertAuthorizationIsKept(filter, authentication, "/agent/session/ask");
        assertAuthorizationIsKept(filter, authentication, "/api/agent/speech/transcribe");
        assertAuthorizationIsKept(filter, authentication, "/product-a/agent/session/ask");
        assertAuthorizationIsKept(filter, authentication, "/product-b/api/agent/speech/transcribe");
    }

    @Test
    void shouldStripAuthorizationForUnconfiguredProductLikePath() {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setTenantPathIds(Map.of("product-a", "tenant-1"));
        UserInfoForwardFilter filter = new UserInfoForwardFilter(properties, trustedForwardProperties());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin-id", null, List.of());
        authentication.setDetails(Map.of("tenantId", "tenant-1", "superAdmin", true));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/unknown-product/api/agent/session/ask")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
                    forwarded.set(chainExchange);
                    return Mono.empty();
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        assertThat(forwarded.get().getRequest().getHeaders().containsHeader(HttpHeaders.AUTHORIZATION))
                .isFalse();
    }

    private void assertAuthorizationIsKept(
            UserInfoForwardFilter filter,
            UsernamePasswordAuthenticationToken authentication,
            String path
    ) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
                    forwarded.set(chainExchange);
                    return Mono.empty();
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer token");
    }

    private TrustedForwardProperties trustedForwardProperties() {
        TrustedForwardProperties properties = new TrustedForwardProperties();
        properties.setToken("trusted-token");
        return properties;
    }
}
