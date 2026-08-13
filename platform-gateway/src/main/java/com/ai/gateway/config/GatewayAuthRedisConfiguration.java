package com.ai.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * 网关读取认证共享 Redis 的专用连接。
 * <p>
 * 网关默认 Redis 继续用于网关自身限流；JWT 黑名单和用户认证概要使用共享认证 DB。
 * </p>
 */
@Configuration(proxyBeanMethods = false)
public class GatewayAuthRedisConfiguration {

    public static final String REDIS_CONNECTION_FACTORY = "redisConnectionFactory";
    public static final String REACTIVE_STRING_REDIS_TEMPLATE = "reactiveStringRedisTemplate";
    public static final String AUTH_REACTIVE_REDIS_CONNECTION_FACTORY = "authReactiveRedisConnectionFactory";
    public static final String AUTH_REACTIVE_STRING_REDIS_TEMPLATE = "authReactiveStringRedisTemplate";

    @Bean(REDIS_CONNECTION_FACTORY)
    @Primary
    @ConditionalOnMissingBean(name = REDIS_CONNECTION_FACTORY)
    public LettuceConnectionFactory redisConnectionFactory(DataRedisProperties properties) {
        return createConnectionFactory(
                properties,
                properties.getHost(),
                properties.getPort(),
                properties.getDatabase(),
                properties.getUsername(),
                properties.getPassword()
        );
    }

    @Bean(REACTIVE_STRING_REDIS_TEMPLATE)
    @Primary
    @ConditionalOnMissingBean(name = REACTIVE_STRING_REDIS_TEMPLATE)
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            @Qualifier(REDIS_CONNECTION_FACTORY) ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }

    @Bean(AUTH_REACTIVE_REDIS_CONNECTION_FACTORY)
    public ReactiveRedisConnectionFactory authReactiveRedisConnectionFactory(
            DataRedisProperties properties,
            Environment environment) {
        return createConnectionFactory(
                properties,
                environment.getProperty("ai.security.redis.host", properties.getHost()),
                environment.getProperty("ai.security.redis.port", Integer.class, properties.getPort()),
                environment.getProperty("ai.security.redis.database", Integer.class, properties.getDatabase()),
                environment.getProperty("ai.security.redis.username", properties.getUsername()),
                environment.getProperty("ai.security.redis.password", properties.getPassword())
        );
    }

    @Bean(AUTH_REACTIVE_STRING_REDIS_TEMPLATE)
    public ReactiveStringRedisTemplate authReactiveStringRedisTemplate(
            @Qualifier(AUTH_REACTIVE_REDIS_CONNECTION_FACTORY) ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }

    private LettuceConnectionFactory createConnectionFactory(
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
