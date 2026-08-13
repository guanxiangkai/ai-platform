package com.ai.gateway.handler;

import com.ai.gateway.util.ReactiveResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * 网关全局异常处理器
 * <p>
 * 捕获网关层（路由失败、服务不可达、超时等）异常，
 * 通过 {@link ReactiveResponseUtils} 统一返回 {@link io.github.guanxiangkai.web.plus.core.model.ApiResponse} JSON 格式。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Order(-2)
@Component
public class GatewayExceptionHandler implements WebExceptionHandler {

    @Override
    public @NonNull Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        int code;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            code = rse.getStatusCode().value();
            message = switch (code) {
                case 404 -> "服务路由未找到";
                case 503 -> "服务暂不可用，请稍后重试";
                case 504 -> "服务响应超时";
                default -> rse.getReason() != null ? rse.getReason() : rse.getStatusCode().toString();
            };
        } else if (ex.getMessage() != null && ex.getMessage().contains("Connection refused")) {
            code = 503;
            message = "目标服务不可达，请检查服务是否已启动";
        } else {
            code = 500;
            message = "网关内部错误";
        }

        log.error("[Gateway] 请求 [{}] 异常: {} - {}",
                exchange.getRequest().getURI().getPath(), code, message, ex);

        return ReactiveResponseUtils.writeError(exchange, code, message);
    }
}
