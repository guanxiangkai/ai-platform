package com.ai.system;

import com.ai.system.config.SystemQueryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * AI 系统服务启动类
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@EnableDiscoveryClient
@EnableConfigurationProperties(SystemQueryProperties.class)
@SpringBootApplication(excludeName = {
        "io.github.guanxiangkai.redis.plus.autoconfigure.governance.RedisPlusGovernanceAutoConfiguration",
        "org.springframework.boot.data.redis.autoconfigure.health.DataRedisHealthContributorAutoConfiguration"
})
public class SystemApplication {

    static void main(String[] args) {
        SpringApplication application = new SpringApplication(SystemApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ AI 系统服务 启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
