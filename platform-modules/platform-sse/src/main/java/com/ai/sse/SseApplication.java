package com.ai.sse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * SSE 推送服务启动类
 * <p>
 * 专职负责维护客户端长连接（Server-Sent Events），
 * 并接收来自其他微服务（通过 MQ）的消息推送给在线用户。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@EnableDiscoveryClient
@SpringBootApplication
public class SseApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(SseApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ AI SSE 推送服务 启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
