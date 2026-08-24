package com.ai.gateway.filter;

import com.ai.gateway.constant.FilterOrder;
import com.ai.gateway.util.ReactiveRequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关请求日志过滤器
 * <p>
 * 职责：
 * <ol>
 *   <li>记录请求入口日志（方法、路径、来源IP）</li>
 *   <li>在成功、失败和取消路径记录请求耗时</li>
 * </ol>
 * IP 获取复用 {@link ReactiveRequestUtils#getClientIp}。
 *
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
        var request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String clientIp = ReactiveRequestUtils.getClientIp(request);
        long startTime = System.currentTimeMillis();

        log.info(">>> {} {} from {}", method, path, clientIp);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;
                    log.info("<<< {} {} → {} ({}ms, {})",
                            method, path, statusCode, duration, signalType);
                });
    }
}
