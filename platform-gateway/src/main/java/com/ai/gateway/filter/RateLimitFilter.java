package com.ai.gateway.filter;

import com.ai.gateway.config.AiGatewayProperties;
import com.ai.gateway.constant.FilterOrder;
import com.ai.gateway.util.ReactiveRequestUtils;
import com.ai.gateway.util.ReactiveResponseUtils;
import io.github.guanxiangkai.web.plus.core.crypto.SecurityFingerprint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关全局限流过滤器（基于 Redis 固定窗口计数器）
 * <p>
 * 多维度防护：
 * <ol>
 *   <li><b>IP 黑名单</b> — 命中直接拒绝</li>
 *   <li><b>登录接口防暴力破解</b> — 单 IP 对登录接口限频</li>
 *   <li><b>IP 维度限流</b> — 单 IP 每分钟最多 N 次请求</li>
 *   <li><b>用户维度限流</b> — 从 {@link ReactiveSecurityContextHolder} 获取已认证用户 ID</li>
 * </ol>
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final AiGatewayProperties props;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public int getOrder() {
        return FilterOrder.RATE_LIMIT;
    }

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!props.isRateLimitEnabled()) {
            return chain.filter(exchange);
        }

        String clientIp = ReactiveRequestUtils.getClientIp(exchange.getRequest(), props.getTrustedProxyIps());
        String path = exchange.getRequest().getURI().getPath();

        // 1. IP 黑名单检查
        if (props.getIpBlacklist().contains(clientIp)) {
            log.warn("[限流] 客户端命中 IP 黑名单: subject={}, path={}", fingerprint(clientIp), path);
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.FORBIDDEN, "您的 IP 已被限制访问");
        }

        // 2. 登录接口防暴力破解
        if ("/auth/login".equals(path) || "/api/auth/login".equals(path)) {
            String loginKey = props.getRateLimitKeyPrefix() + "login:" + fingerprint(clientIp);
            return checkLimit(loginKey, props.getLoginMaxAttempts(), props.getLoginWindowSeconds(), path, "登录防暴破")
                    .flatMap(ok -> ok ? checkIpLimit(clientIp, path, exchange, chain)
                            : ReactiveResponseUtils.writeTooManyRequests(exchange, "登录尝试过于频繁，请稍后再试", props.getLoginWindowSeconds()));
        }

        // 3. IP 维度限流
        return checkIpLimit(clientIp, path, exchange, chain);
    }

    private Mono<Void> checkIpLimit(String clientIp, String path, ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = props.getRateLimitKeyPrefix() + "ip:" + fingerprint(clientIp);
        return checkLimit(key, props.getIpMaxRequests(), props.getIpWindowSeconds(), path, "IP")
                .flatMap(ok -> {
                    if (!ok) {
                        return ReactiveResponseUtils.writeTooManyRequests(exchange, "请求过于频繁，请稍后再试", props.getIpWindowSeconds());
                    }

                    // 4. 用户维度限流 — 从 SecurityContext 获取用户 ID
                    return ReactiveSecurityContextHolder.getContext()
                            .flatMap(ctx -> Mono.justOrEmpty(ctx.getAuthentication()))
                            .filter(Authentication::isAuthenticated)
                            .flatMap(auth -> {
                                String userId = auth.getName();
                                String userKey = props.getRateLimitKeyPrefix() + "user:" + fingerprint(userId);
                                return checkLimit(userKey, props.getUserMaxRequests(), props.getUserWindowSeconds(), path, "用户")
                                        .flatMap(uOk -> uOk ? chain.filter(exchange)
                                                : ReactiveResponseUtils.writeTooManyRequests(exchange, "操作过于频繁，请稍后再试", props.getUserWindowSeconds()))
                                        .thenReturn(Boolean.TRUE);
                            })
                            // 未认证请求（白名单路径），跳过用户维度限流
                            .switchIfEmpty(Mono.defer(() -> chain.filter(exchange).thenReturn(Boolean.FALSE)))
                            .then();
                });
    }

    /**
     * 限流 Lua 脚本（原子操作）
     * <p>
     * 1. INCR 递增计数器
     * 2. 若 key 无过期时间（TTL == -1），则设置过期时间
     * <p>
     * 解决非原子操作下 INCR 成功但 EXPIRE 未执行导致 key 永不过期、永久限流的问题
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

    /**
     * 基于 Redis Lua 脚本的固定窗口计数器（原子操作）
     *
     * @return Mono(true)=放行，Mono(false)=限流
     */
    private Mono<Boolean> checkLimit(String key, int max, int windowSec, String path, String dimension) {
        return redisTemplate.execute(INCR_WITH_EXPIRE_SCRIPT, List.of(key), String.valueOf(windowSec))
                .single()
                .map(count -> {
                    if (count > max) {
                        log.warn("[限流] {} 维度触发: path={}, count={}/{}, window={}s",
                                dimension, path, count, max, windowSec);
                        return false;
                    }
                    return true;
                })
                .onErrorResume(ex -> {
                    log.error("[限流] Redis 计数异常: dim={}, path={}, failOpen={}, exception={}",
                            dimension, path, props.isRateLimitFailOpen(), ex.getClass().getSimpleName());
                    if (props.isRateLimitFailOpen()) {
                        return Mono.just(true);
                    }
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE, "限流服务暂不可用", ex));
                });
    }

    private String fingerprint(String value) {
        return SecurityFingerprint.sha256(value);
    }
}
