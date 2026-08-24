package com.ai.gateway.constant;

import java.util.List;

/**
 * Gateway 模块常量
 */
public final class GatewayConstants {

    private GatewayConstants() {
        throw new UnsupportedOperationException("这是一个效用类，无法实例化");
    }

    /**
     * 请求头常量
     */
    public static final class HeaderConstants {

        public static final List<String> CLIENT_IP_HEADERS = List.of(
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_CLIENT_IP"
        );

        private HeaderConstants() {
        }
    }

}
