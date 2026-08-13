package com.ai.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Auth 服务 Reactor Netty 请求解码配置。
 */
@ConfigurationProperties(prefix = "ai.auth.netty")
public record AuthNettyProperties(
        int maxInitialLineLength,
        int maxHeaderSize
) {

    public AuthNettyProperties {
        if (maxInitialLineLength <= 0) {
            maxInitialLineLength = 32 * 1024;
        }
        if (maxHeaderSize <= 0) {
            maxHeaderSize = 64 * 1024;
        }
    }

    public AuthNettyProperties() {
        this(0, 0);
    }
}
