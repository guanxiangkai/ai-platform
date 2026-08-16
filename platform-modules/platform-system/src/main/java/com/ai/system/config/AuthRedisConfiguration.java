package com.ai.system.config;

import io.github.guanxiangkai.redis.plus.datasource.LettuceConnectionFactoryBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * system 写入认证概要、消费操作日志 Stream 的共享 Redis 连接。
 */
@Configuration(proxyBeanMethods = false)
public class AuthRedisConfiguration {

    public static final String REDIS_CONNECTION_FACTORY = "redisConnectionFactory";
    public static final String STRING_REDIS_TEMPLATE = "stringRedisTemplate";
    public static final String AUTH_REDIS_CONNECTION_FACTORY = "authRedisConnectionFactory";
    public static final String AUTH_STRING_REDIS_TEMPLATE = "authStringRedisTemplate";

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
        LettuceConnectionFactoryBuilder builder = LettuceConnectionFactoryBuilder
                .standalone(host, port)
                .database(database)
                .username(username)
                .password(password)
                .clientName(properties.getClientName())
                .ssl(properties.getSsl().isEnabled(), false, true);
        if (properties.getTimeout() != null) {
            builder.commandTimeout(properties.getTimeout());
        }
        if (properties.getConnectTimeout() != null) {
            builder.connectTimeout(properties.getConnectTimeout());
        }
        if (properties.getLettuce().getShutdownTimeout() != null) {
            builder.shutdownTimeout(properties.getLettuce().getShutdownTimeout());
        }
        return builder.build();
    }
}
