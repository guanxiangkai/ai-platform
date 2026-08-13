package com.ai.system.config;

import io.github.guanxiangkai.redis.plus.core.redis.RedisBackend;
import io.github.guanxiangkai.redis.plus.core.redis.StringRedisBackend;
import io.github.guanxiangkai.redis.plus.core.script.DefaultRedisScriptExecutor;
import io.github.guanxiangkai.redis.plus.core.script.RedisScriptExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * 为 redis-plus 提供基础 RedisConnectionFactory，避免其多数据源工厂在回退到单数据源模式时
 * 与 Spring Data Redis 自动配置互相竞争同一个 RedisConnectionFactory 类型导致循环依赖。
 */
@Configuration
public class SystemRedisConfiguration {

    @Bean("redisConnectionFactory")
    @Primary
    @ConditionalOnMissingBean(name = "redisConnectionFactory")
    public RedisConnectionFactory redisConnectionFactory(DataRedisProperties properties) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
        standalone.setHostName(properties.getHost());
        standalone.setPort(properties.getPort());
        standalone.setDatabase(properties.getDatabase());
        if (StringUtils.hasText(properties.getUsername())) {
            standalone.setUsername(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            standalone.setPassword(RedisPassword.of(properties.getPassword()));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder = LettuceClientConfiguration.builder();
        if (properties.getTimeout() != null) {
            clientBuilder.commandTimeout(properties.getTimeout());
        }

        return new LettuceConnectionFactory(standalone, clientBuilder.build());
    }

    @Bean
    @ConditionalOnMissingBean(RedisBackend.class)
    public RedisBackend redisBackend(StringRedisTemplate stringRedisTemplate) {
        return new StringRedisBackend(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(RedisScriptExecutor.class)
    public RedisScriptExecutor redisScriptExecutor(RedisBackend redisBackend) {
        return new DefaultRedisScriptExecutor(redisBackend);
    }
}
