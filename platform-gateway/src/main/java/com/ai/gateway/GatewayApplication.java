package com.ai.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * AI 网关服务启动类
 * <p>
 * 基于 Spring Cloud Gateway 的 API 网关，负责路由转发、负载均衡、统一鉴权等。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@EnableDiscoveryClient
@SpringBootApplication(excludeName = {
        "org.springframework.cloud.gateway.config.GatewayRedisAutoConfiguration"
})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ AI 网关服务 启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
