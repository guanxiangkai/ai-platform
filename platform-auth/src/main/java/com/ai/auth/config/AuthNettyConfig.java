package com.ai.auth.config;

import com.ai.auth.properties.AuthNettyProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.reactor.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auth 服务 Reactor Netty 服务端配置。
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class AuthNettyConfig {

    @Bean
    public NettyServerCustomizer authHttpRequestDecoderCustomizer(AuthNettyProperties properties) {
        return httpServer -> httpServer.httpRequestDecoder(spec -> spec
                .maxInitialLineLength(properties.maxInitialLineLength())
                .maxHeaderSize(properties.maxHeaderSize()));
    }
}
