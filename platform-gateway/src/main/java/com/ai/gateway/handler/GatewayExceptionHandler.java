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

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

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
            message = publicMessage(code);
        } else if (hasCause(ex, ConnectException.class)) {
            code = 503;
            message = "服务暂不可用，请稍后重试";
        } else if (hasCause(ex, TimeoutException.class)) {
            code = 504;
            message = "服务响应超时";
        } else {
            code = 500;
            message = "网关内部错误";
        }

        log.error("[Gateway] 请求处理异常: path={}, status={}, exception={}",
                exchange.getRequest().getURI().getPath(), code, ex.getClass().getSimpleName());

        return ReactiveResponseUtils.writeError(exchange, code, message);
    }

    private static String publicMessage(int code) {
        return switch (code) {
            case 400 -> "请求无效";
            case 401 -> "认证失败";
            case 403 -> "无权访问";
            case 404 -> "服务路由未找到";
            case 405 -> "请求方法不受支持";
            case 408 -> "请求超时";
            case 413 -> "请求内容过大";
            case 415 -> "请求内容类型不受支持";
            case 429 -> "请求过于频繁，请稍后重试";
            case 502, 503 -> "服务暂不可用，请稍后重试";
            case 504 -> "服务响应超时";
            default -> code >= 400 && code < 500 ? "请求处理失败" : "网关内部错误";
        };
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
