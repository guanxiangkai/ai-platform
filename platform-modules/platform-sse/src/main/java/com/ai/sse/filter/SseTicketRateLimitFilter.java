package com.ai.sse.filter;

import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import com.ai.sse.config.SseProperties;
import com.ai.sse.constants.SseConstants;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * SSE 票据接口限流过滤器
 * <p>
 * 基于 Redis 固定窗口对 {@code POST /sse/ticket} 进行每用户限流，
 * 防止恶意/频繁请求大量生成一次性票据。
 * </p>
 * <p>
 * 限流策略：固定窗口（Lua 原子 INCR + EXPIRE），
 * 每 {@code ticketRateWindow} 秒内最多 {@code ticketRateLimit} 次请求。
 * 使用 {@link ReactiveStringRedisTemplate} 保持全链路无阻塞。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@NullMarked
public class SseTicketRateLimitFilter implements WebFilter, Ordered {

    /**
     * 原子固定窗口 Lua 脚本：INCR 后仅在 key 无 TTL 时设置过期，避免 INCR+EXPIRE 非原子导致 key 永不过期。
     */
    private static final RedisScript<Long> INCR_WITH_EXPIRE_SCRIPT = RedisScript.of(
            """
                    local count = redis.call('INCR', KEYS[1])
                    if redis.call('TTL', KEYS[1]) < 0 then
                        redis.call('EXPIRE', KEYS[1], ARGV[1])
                    end
                    return count
                    """,
            Long.class
    );

    private final ReactiveStringRedisTemplate redisTemplate;
    private final SseProperties sseProperties;
    private final ObjectMapper objectMapper;

    public SseTicketRateLimitFilter(
            ReactiveStringRedisTemplate redisTemplate,
            SseProperties sseProperties,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.sseProperties = sseProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 仅拦截 POST /sse/ticket
        if (!HttpMethod.POST.equals(request.getMethod())
                || !"/sse/ticket".equals(request.getPath().value())) {
            return chain.filter(exchange);
        }

        // 获取用户标识（从网关转发的请求头中）
        String userId = request.getHeaders().getFirst(AuthConstants.HeaderConstants.USER_ID);
        if (userId == null || userId.isBlank()) {
            // 未认证的请求，交给后续安全过滤器处理
            return chain.filter(exchange);
        }

        String key = SseConstants.RedisKeyConstants.TICKET_RATE_LIMIT_PREFIX + userId;
        int limit = sseProperties.ticketRateLimit();
        long windowSeconds = Math.max(1, sseProperties.ticketRateWindow() / 1000);

        return redisTemplate.execute(INCR_WITH_EXPIRE_SCRIPT, List.of(key), String.valueOf(windowSeconds))
                .single()
                .flatMap(count -> {
                    if (count > limit) {
                        log.warn("[SSE-RateLimit] 票据请求限流: userId={}, count={}, limit={}/{}s",
                                userId, count, limit, windowSeconds);
                        return rejectResponse(exchange);
                    }
                    return chain.filter(exchange);
                });
    }

    /**
     * 返回 429 Too Many Requests
     */
    private Mono<Void> rejectResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(
                        ApiResponse.fail(HttpStatus.TOO_MANY_REQUESTS.value(), "请求过于频繁，请稍后再试")))
                .flatMap(body -> response.writeWith(Mono.just(response.bufferFactory().wrap(body))));
    }

    @Override
    public int getOrder() {
        // 在认证过滤器之后执行（需要 X-User-Id 头）
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
