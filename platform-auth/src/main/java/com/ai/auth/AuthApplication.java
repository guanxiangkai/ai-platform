package com.ai.auth;

import com.ai.auth.properties.AuthProtectionProperties;
import com.ai.auth.properties.AuthNettyProperties;
import com.ai.auth.properties.AuthSuperAdminProperties;
import com.ai.auth.properties.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * AI 认证服务启动类
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@EnableDiscoveryClient
@EnableConfigurationProperties({
        JwtProperties.class,
        AuthProtectionProperties.class,
        AuthNettyProperties.class,
        AuthSuperAdminProperties.class
})
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AuthApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ AI 认证服务 启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
