package com.ai.system.config;

import com.ai.api.security.ProtocolPasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 系统用户生命周期使用的密码摘要协议编码器配置。 */
@Configuration(proxyBeanMethods = false)
public class SystemPasswordConfiguration {

    /** 系统仅接受客户端 SHA-1 摘要，并以标准裸 BCrypt 存储。 */
    @Bean
    @Primary
    public PasswordEncoder protocolPasswordEncoder() {
        return new ProtocolPasswordEncoder();
    }
}
