package com.ai.auth.config;

import com.ai.api.security.ProtocolPasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 平台认证服务的密码摘要协议编码器配置。 */
@Configuration(proxyBeanMethods = false)
public class AuthPasswordConfiguration {

    /** 认证仅接受客户端 SHA-1 摘要，并以带协议标识的 BCrypt 存储。 */
    @Bean
    @Primary
    public PasswordEncoder protocolPasswordEncoder() {
        return new ProtocolPasswordEncoder();
    }
}
