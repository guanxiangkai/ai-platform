package com.ai.gateway.filter;

import com.ai.gateway.config.AiGatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRouteGuardFilterTest {

    @Test
    void shouldAllowMatchingTenantBusinessRoute() {
        AiGatewayProperties properties = properties();
        TenantRouteGuardFilter filter = new TenantRouteGuardFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/alpha/agent/session/1").build()
        );
        AtomicBoolean forwarded = new AtomicBoolean();
        AtomicInteger invocationCount = new AtomicInteger();

        filter.filter(exchange, ignored -> {
                    invocationCount.incrementAndGet();
                    forwarded.set(true);
                    return Mono.empty();
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication("tenant-alpha")))
                .block();

        assertThat(forwarded).isTrue();
        assertThat(invocationCount).hasValue(1);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldRejectCrossTenantBusinessRoute() {
        TenantRouteGuardFilter filter = new TenantRouteGuardFilter(properties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/alpha/catalog/items").build()
        );
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
                    forwarded.set(true);
                    return Mono.empty();
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication("tenant-beta")))
                .block();

        assertThat(forwarded).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldIgnoreSharedPlatformRoute() {
        TenantRouteGuardFilter filter = new TenantRouteGuardFilter(properties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/system/user/profile").build()
        );
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();

        assertThat(forwarded).isTrue();
    }

    @Test
    void shouldAllowAnonymousTenantLoginRoute() {
        TenantRouteGuardFilter filter = new TenantRouteGuardFilter(properties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/beta/auth/login").build()
        );
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();

        assertThat(forwarded).isTrue();
    }

    @Test
    void shouldAllowAnonymousProductApiCryptoConfigRoute() {
        TenantRouteGuardFilter filter = new TenantRouteGuardFilter(properties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/beta/api/web-plus/api-crypto/config").build()
        );
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();

        assertThat(forwarded).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldRejectAnonymousAdjacentWebPlusRoute() {
        TenantRouteGuardFilter filter = new TenantRouteGuardFilter(properties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/beta/api/web-plus/internal/config").build()
        );
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();

        assertThat(forwarded).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private AiGatewayProperties properties() {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setTenantPathIds(Map.of(
                "alpha", "tenant-alpha",
                "beta", "tenant-beta"
        ));
        properties.setExcludePaths(List.of(
                "/beta/auth/login",
                "/beta/api/web-plus/api-crypto/config"
        ));
        return properties;
    }

    private UsernamePasswordAuthenticationToken authentication(String tenantId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("user-1", null, List.of());
        authentication.setDetails(Map.of("tenantId", tenantId, "superAdmin", false));
        return authentication;
    }
}
