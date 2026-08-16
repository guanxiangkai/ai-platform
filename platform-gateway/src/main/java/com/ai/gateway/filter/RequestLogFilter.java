package com.ai.gateway.filter;

import com.ai.gateway.constant.FilterOrder;
import com.ai.gateway.constant.GatewayConstants;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 全局请求日志 & 链路追踪过滤器
 * <p>
 * 职责：
 * <ol>
 *   <li>为每个请求生成唯一 traceId 并写入请求头</li>
 *   <li>记录请求入口日志（方法、路径）</li>
 *   <li>记录请求耗时</li>
 * </ol>
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return FilterOrder.REQUEST_LOG;
    }

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        log.info("[{}] >>> {} {}", traceId, method, path);

        exchange.getAttributes().put(GatewayConstants.ExchangeAttributeConstants.START_TIME, System.currentTimeMillis());

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(GatewayConstants.HeaderConstants.TRACE_ID, traceId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    Long startTime = exchange.getAttribute(GatewayConstants.ExchangeAttributeConstants.START_TIME);
                    long duration = startTime != null ? System.currentTimeMillis() - startTime : -1;
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;
                    log.info("[{}] <<< {} {} → {} ({}ms)", traceId, method, path, statusCode, duration);
                }));
    }
}
