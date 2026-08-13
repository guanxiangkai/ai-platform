package com.ai.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 注册可靠出站任务调度配置。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(RegisterOutboxProperties.class)
public class RegisterOutboxConfiguration {
}
