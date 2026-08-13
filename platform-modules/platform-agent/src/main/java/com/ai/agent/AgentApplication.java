package com.ai.agent;

import com.ai.agent.config.AgentAsrProperties;
import com.ai.agent.config.AgentInvocationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 跨产品、跨租户的通用智能体服务启动入口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableConfigurationProperties({AgentAsrProperties.class, AgentInvocationProperties.class})
public class AgentApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AgentApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
    }
}
