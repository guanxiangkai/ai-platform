package com.ai.gateway.util;

import cn.hutool.json.JSONUtil;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 响应式响应写入工具类
 * <p>
 * 统一 JSON 错误响应写入逻辑，复用 {@link ApiResponse}（web-plus-core）
 * 和 Hutool {@link JSONUtil} 序列化，避免各 Filter / Handler 重复拼装。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class ReactiveResponseUtils {


    private ReactiveResponseUtils() {
    }

    /**
     * 写入错误 JSON 响应
     *
     * @param exchange 请求上下文
     * @param status   HTTP 状态码
     * @param message  错误消息
     * @return Mono&lt;Void&gt;
     */
    public static Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        return writeError(exchange, status.value(), message);
    }

    /**
     * 写入错误 JSON 响应（int 状态码）
     *
     * @param exchange 请求上下文
     * @param code     HTTP 状态码数值
     * @param message  错误消息
     * @return Mono&lt;Void&gt;
     */
    public static Mono<Void> writeError(ServerWebExchange exchange, int code, String message) {
        return Mono.defer(() -> {
            ServerHttpResponse response = exchange.getResponse();
            if (response.isCommitted()) {
                return Mono.empty();
            }
            response.setStatusCode(HttpStatusCode.valueOf(code));
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            // 复用 web-plus-core 的 ApiResponse + Hutool JSONUtil，保持全链路响应格式统一
            ApiResponse<Void> apiResponse = ApiResponse.fail(code, message);
            byte[] bytes = JSONUtil.toJsonStr(apiResponse).getBytes(StandardCharsets.UTF_8);

            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        });
    }

    /**
     * 写入 429 限流响应（附带 Retry-After 头）
     */
    public static Mono<Void> writeTooManyRequests(ServerWebExchange exchange, String message, int retryAfterSeconds) {
        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(retryAfterSeconds));
        return writeError(exchange, HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
