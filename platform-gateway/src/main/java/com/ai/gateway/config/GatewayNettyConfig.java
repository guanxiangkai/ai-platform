package com.ai.gateway.config;

import org.springframework.boot.reactor.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关 Reactor Netty 服务端配置。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Configuration
public class GatewayNettyConfig {

    @Bean
    public NettyServerCustomizer gatewayHttpRequestDecoderCustomizer(AiGatewayProperties properties) {
        return httpServer -> {
            AiGatewayProperties.Netty netty = properties.getNetty();
            return httpServer.httpRequestDecoder(spec -> spec
                    .maxInitialLineLength(netty.getMaxInitialLineLength())
                    .maxHeaderSize(netty.getMaxHeaderSize()));
        };
    }
}
