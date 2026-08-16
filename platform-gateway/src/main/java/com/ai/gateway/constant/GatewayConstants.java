package com.ai.gateway.constant;

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

        public static final String TRACE_ID = "X-Trace-Id";
        private HeaderConstants() {
        }
    }

    /**
     * Exchange 属性常量
     */
    public static final class ExchangeAttributeConstants {

        public static final String START_TIME = "gateway_start_time";

        private ExchangeAttributeConstants() {
        }
    }
}
