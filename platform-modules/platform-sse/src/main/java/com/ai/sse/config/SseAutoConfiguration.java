package com.ai.sse.config;

import com.ai.sse.aot.SseRuntimeHints;
import com.ai.sse.core.DefaultSseOperations;
import com.ai.sse.core.RedisSseMessageBridge;
import com.ai.sse.core.SseOperations;
import com.ai.sse.filter.SseTicketRateLimitFilter;
import com.ai.sse.handler.HeartbeatHandler;
import com.ai.sse.handler.SseMessageDispatcher;
import com.ai.sse.scheduler.SseHeartbeatScheduler;
import com.ai.sse.service.ISseService;
import com.ai.sse.service.impl.SseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.ObjectMapper;

/**
 * SSE 自动配置
 * <p>
 * 装配核心 Bean：{@link SseOperations}、{@link HeartbeatHandler}、{@link ISseService}，
 * 以及安全增强组件：限流过滤器、心跳调度器、Redis PubSub 消息桥。
 * 并启动定时清理任务回收超时连接。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties(SseProperties.class)
@ImportRuntimeHints(SseRuntimeHints.class)
public class SseAutoConfiguration {

    // ==================== 核心 Bean ====================

    /**
     * SSE 操作核心实现（支持多连接/用户、服务端心跳）
     */
    @Bean
    @ConditionalOnMissingBean(SseOperations.class)
    public SseOperations sseOperations(SseProperties properties, ObjectMapper objectMapper,
                                       ApplicationEventPublisher eventPublisher) {
        return new DefaultSseOperations(
                properties.timeout(),
                properties.maxConnections(),
                properties.maxConnectionsPerUser(),
                objectMapper,
                eventPublisher
        );
    }

    /**
     * 心跳处理器（客户端主动心跳的响应处理器）
     */
    @Bean
    @ConditionalOnMissingBean(HeartbeatHandler.class)
    public HeartbeatHandler heartbeatHandler(SseOperations sseOperations) {
        return new HeartbeatHandler(sseOperations);
    }

    /**
     * SSE 服务门面（可选注入 PubSub 消息桥）
     */
    @Bean
    @ConditionalOnMissingBean(ISseService.class)
    public ISseService sseService(SseOperations sseOperations, SseMessageDispatcher dispatcher,
                                  @Nullable RedisSseMessageBridge messageBridge) {
        return new SseServiceImpl(sseOperations, dispatcher, messageBridge);
    }

    // ==================== 安全增强 ====================

    /**
     * 票据接口限流过滤器（全链路响应式：ReactiveStringRedisTemplate + Lua 原子计数）
     */
    @Bean
    public SseTicketRateLimitFilter sseTicketRateLimitFilter(ReactiveStringRedisTemplate redisTemplate,
                                                             SseProperties sseProperties,
                                                             ObjectMapper objectMapper) {
        log.info("[SSE] 票据限流已启用: limit={}/{}ms",
                sseProperties.ticketRateLimit(), sseProperties.ticketRateWindow());
        return new SseTicketRateLimitFilter(redisTemplate, sseProperties, objectMapper);
    }

    // ==================== 服务端心跳 ====================

    /**
     * 服务端心跳调度器（定时向所有在线连接推送 keepalive）
     */
    @Bean
    public SseHeartbeatScheduler sseHeartbeatScheduler(SseOperations sseOperations) {
        log.info("[SSE] 服务端心跳调度器已启用");
        return new SseHeartbeatScheduler(sseOperations);
    }

    // ==================== 多实例路由（Redis PubSub） ====================

    /**
     * Redis PubSub 消息桥
     */
    @Bean
    @ConditionalOnProperty(name = "ai.sse.enable-pubsub", havingValue = "true", matchIfMissing = true)
    public RedisSseMessageBridge redisSseMessageBridge(SseOperations sseOperations,
                                                       StringRedisTemplate redisTemplate,
                                                       ObjectMapper objectMapper,
                                                       SseProperties sseProperties) {
        return new RedisSseMessageBridge(sseOperations, redisTemplate, objectMapper, sseProperties);
    }

    /**
     * Redis 消息监听容器（PubSub 订阅）
     */
    @Bean
    @ConditionalOnProperty(name = "ai.sse.enable-pubsub", havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisSseMessageBridge messageBridge) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageBridge, new ChannelTopic(messageBridge.getChannel()));
        log.info("[SSE-PubSub] Redis 订阅已启动: channel={}, instanceId={}",
                messageBridge.getChannel(), messageBridge.getInstanceId());
        return container;
    }

    // ==================== 定时清理 ====================

    /**
     * 超时连接定时清理任务
     */
    @Bean
    SseConnectionCleanupTask sseConnectionCleanupTask(SseOperations sseOperations) {
        return new SseConnectionCleanupTask(sseOperations);
    }


    static class SseConnectionCleanupTask {

        private final SseOperations sseOperations;

        SseConnectionCleanupTask(SseOperations sseOperations) {
            this.sseOperations = sseOperations;
        }

        /**
         * 定时清理超时连接（默认每 60 秒执行一次）
         */
        @Scheduled(fixedDelayString = "${ai.sse.cleanup-interval:60000}")
        public void cleanup() {
            if (sseOperations instanceof DefaultSseOperations ops) {
                int cleaned = ops.cleanupExpiredConnections();
                if (cleaned > 0) {
                    log.info("[SSE-Cleanup] 清理超时连接: count={}", cleaned);
                }
            }
        }
    }
}
