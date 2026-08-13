package com.ai.gateway.filter;

import com.ai.gateway.config.AiGatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    @Test
    void redisFailureSkipsRateLimitAndContinuesChain() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.<List<String>>any(), anyString()))
                .thenReturn(Flux.error(new IllegalStateException("redis connection reset")));
        RateLimitFilter filter = new RateLimitFilter(new AiGatewayProperties(), redis);
        MockServerWebExchange exchange = loginExchange();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = chainExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void exceededLoginLimitReturnsTooManyRequests() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.<List<String>>any(), anyString()))
                .thenReturn(Flux.just(11L));
        RateLimitFilter filter = new RateLimitFilter(new AiGatewayProperties(), redis);
        MockServerWebExchange exchange = loginExchange();

        filter.filter(exchange, chainExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("300");
    }

    @Test
    void apiAuthLoginUsesLoginLimit() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.<List<String>>any(), anyString()))
                .thenReturn(Flux.just(11L));
        RateLimitFilter filter = new RateLimitFilter(new AiGatewayProperties(), redis);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/login")
                .header("X-Forwarded-For", "192.0.2.10")
                .build());

        filter.filter(exchange, chainExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void authenticatedRequestContinuesChainOnlyOnce() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.<List<String>>any(), anyString()))
                .thenReturn(Flux.just(1L));
        RateLimitFilter filter = new RateLimitFilter(new AiGatewayProperties(), redis);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/system/user/profile")
                .header("X-Forwarded-For", "192.0.2.10")
                .build());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("user-1", null, List.of());
        AtomicInteger invocationCount = new AtomicInteger();

        filter.filter(exchange, chainExchange -> {
                    invocationCount.incrementAndGet();
                    return Mono.empty();
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        assertThat(invocationCount).hasValue(1);
    }

    private static MockServerWebExchange loginExchange() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/auth/login")
                .header("X-Forwarded-For", "192.0.2.10")
                .build();
        return MockServerWebExchange.from(request);
    }
}
