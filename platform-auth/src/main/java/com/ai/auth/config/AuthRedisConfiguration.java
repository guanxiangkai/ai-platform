package com.ai.auth.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * 认证共享 Redis 连接。
 * <p>
 * 默认 Redis 供 auth 自身登录保护等本地能力使用；认证概要、Token 与黑名单
 * 需要被 gateway/system/business 跨服务访问，因此使用独立共享 DB。
 * </p>
 */
@Configuration(proxyBeanMethods = false)
public class AuthRedisConfiguration {

    public static final String REDIS_CONNECTION_FACTORY = "redisConnectionFactory";
    public static final String STRING_REDIS_TEMPLATE = "stringRedisTemplate";
    public static final String AUTH_REDIS_CONNECTION_FACTORY = "authRedisConnectionFactory";
    public static final String AUTH_STRING_REDIS_TEMPLATE = "authStringRedisTemplate";

    @Bean(REDIS_CONNECTION_FACTORY)
    @Primary
    @ConditionalOnMissingBean(name = REDIS_CONNECTION_FACTORY)
    public RedisConnectionFactory redisConnectionFactory(DataRedisProperties properties) {
        return createConnectionFactory(
                properties,
                properties.getHost(),
                properties.getPort(),
                properties.getDatabase(),
                properties.getUsername(),
                properties.getPassword()
        );
    }

    @Bean(STRING_REDIS_TEMPLATE)
    @Primary
    @ConditionalOnMissingBean(name = STRING_REDIS_TEMPLATE)
    public StringRedisTemplate stringRedisTemplate(
            @Qualifier(REDIS_CONNECTION_FACTORY) RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean(AUTH_REDIS_CONNECTION_FACTORY)
    public RedisConnectionFactory authRedisConnectionFactory(DataRedisProperties properties, Environment environment) {
        return createConnectionFactory(
                properties,
                environment.getProperty("ai.security.redis.host", properties.getHost()),
                environment.getProperty("ai.security.redis.port", Integer.class, properties.getPort()),
                environment.getProperty("ai.security.redis.database", Integer.class, properties.getDatabase()),
                environment.getProperty("ai.security.redis.username", properties.getUsername()),
                environment.getProperty("ai.security.redis.password", properties.getPassword())
        );
    }

    @Bean(AUTH_STRING_REDIS_TEMPLATE)
    public StringRedisTemplate authStringRedisTemplate(
            @Qualifier(AUTH_REDIS_CONNECTION_FACTORY) RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    private RedisConnectionFactory createConnectionFactory(
            DataRedisProperties properties,
            String host,
            int port,
            int database,
            String username,
            String password) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
        standalone.setHostName(host);
        standalone.setPort(port);
        standalone.setDatabase(database);
        if (StringUtils.hasText(username)) {
            standalone.setUsername(username);
        }
        if (StringUtils.hasText(password)) {
            standalone.setPassword(RedisPassword.of(password));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder = LettuceClientConfiguration.builder();
        if (properties.getTimeout() != null) {
            clientBuilder.commandTimeout(properties.getTimeout());
        }
        return new LettuceConnectionFactory(standalone, clientBuilder.build());
    }
}
