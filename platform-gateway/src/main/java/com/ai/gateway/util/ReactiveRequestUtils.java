package com.ai.gateway.util;

import io.github.guanxiangkai.web.plus.core.util.IpUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.Collection;

/**
 * 响应式请求工具类
 * <p>
 * 提供在 WebFlux 环境下提取客户端 IP 等通用能力。
 * <br/>
 * 复用 Web Plus 的可信代理算法，避免各过滤器直接信任客户端可伪造的转发请求头。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class ReactiveRequestUtils {

    private ReactiveRequestUtils() {
    }

    /**
     * 按显式可信代理列表获取客户端 IP。
     *
     * @param request 响应式请求对象
     * @param trustedProxyIps 可以提供转发头的精确代理 IP 列表
     * @return 规范化客户端 IP；无法解析时返回 {@code unknown}
     */
    public static String getClientIp(ServerHttpRequest request, Collection<String> trustedProxyIps) {
        return IpUtils.getClientIp(request, trustedProxyIps);
    }
}
