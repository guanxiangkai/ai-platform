package com.ai.gateway.util;

import cn.hutool.core.util.StrUtil;
import com.ai.gateway.constant.GatewayConstants;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * 响应式请求工具类
 * <p>
 * 提供在 WebFlux 环境下提取客户端 IP 等通用能力。
 * <br/>
 * Hutool 的 {@code JakartaServletUtil.getClientIP()} 仅支持 Servlet，
 * 此类参照其实现逻辑适配 {@link ServerHttpRequest}，
 * 复用 {@link StrUtil} 做字符串处理，不重复造轮子。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class ReactiveRequestUtils {

    private ReactiveRequestUtils() {
    }

    /**
     * 获取客户端真实 IP
     * <p>
     * 按优先级遍历代理请求头，取第一个有效值；
     * 均无效时回退到 {@code RemoteAddress}。
     * </p>
     *
     * @param request 响应式请求对象
     * @return 客户端 IP
     */
    public static String getClientIp(ServerHttpRequest request) {
        for (String header : GatewayConstants.HeaderConstants.CLIENT_IP_HEADERS) {
            String ip = request.getHeaders().getFirst(header);
            if (StrUtil.isNotBlank(ip) && !StrUtil.equalsIgnoreCase(ip, "unknown")) {
                // 多级代理时取第一个 IP（如 "1.1.1.1, 2.2.2.2" → "1.1.1.1"）
                return StrUtil.trim(StrUtil.subBefore(ip, ',', false));
            }
        }

        // 兜底：直连 IP
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }
}
