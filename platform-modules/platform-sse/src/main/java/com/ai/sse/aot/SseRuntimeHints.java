package com.ai.sse.aot;

import com.ai.sse.config.SseProperties;
import com.ai.sse.consumer.SseConsumerConfig;
import com.ai.sse.consumer.strategy.BroadcastPushStrategy;
import com.ai.sse.consumer.strategy.TenantPushStrategy;
import com.ai.sse.consumer.strategy.UserPushStrategy;
import com.ai.sse.consumer.strategy.UsersPushStrategy;
import com.ai.sse.controller.SseConnectionRecordController;
import com.ai.sse.controller.SseController;
import com.ai.sse.core.DefaultSseOperations;
import com.ai.sse.core.RedisSseMessageBridge;
import com.ai.sse.domain.entity.SseConnectionRecord;
import com.ai.sse.filter.SseTicketRateLimitFilter;
import com.ai.sse.handler.HeartbeatHandler;
import com.ai.sse.handler.SseMessageDispatcher;
import com.ai.sse.listener.SseConnectionEventListener;
import com.ai.sse.model.SseConnection;
import com.ai.sse.model.SseMessage;
import com.ai.api.sse.dto.SseNotification;
import com.ai.sse.scheduler.SseHeartbeatScheduler;
import com.ai.sse.service.impl.SseConnectionRecordServiceImpl;
import com.ai.sse.service.impl.SseServiceImpl;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.util.Set;

/**
 * SSE 模块 GraalVM Native Image 运行时提示
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public class SseRuntimeHints implements RuntimeHintsRegistrar {

    private static final Set<Class<?>> REFLECTION_CLASSES = Set.of(
            // Record 模型
            SseConnection.class,
            SseMessage.class,
            SseMessage.Builder.class,
            SseNotification.class,
            SseNotification.TargetType.class,

            // 配置
            SseProperties.class,
            SseProperties.PushPreferenceFailurePolicy.class,

            // 核心实现
            DefaultSseOperations.class,
            SseServiceImpl.class,
            SseConnectionRecordServiceImpl.class,

            // 多实例路由
            RedisSseMessageBridge.class,
            RedisSseMessageBridge.PubSubPayload.class,

            // 安全增强
            SseTicketRateLimitFilter.class,
            SseHeartbeatScheduler.class,

            // 连接审计
            SseConnectionRecord.class,
            SseConnectionEventListener.class,

            // 控制器
            SseController.class,
            SseConnectionRecordController.class,

            // 处理器 & 调度器
            HeartbeatHandler.class,
            SseMessageDispatcher.class,

            // MQ 消费者 & 策略
            SseConsumerConfig.class,
            UserPushStrategy.class,
            UsersPushStrategy.class,
            TenantPushStrategy.class,
            BroadcastPushStrategy.class
    );

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // 注册反射提示（使用 Spring Framework 7.0+ 推荐的 MemberCategory）
        REFLECTION_CLASSES.forEach(clazz ->
                hints.reflection().registerType(clazz,
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS,
                        MemberCategory.INVOKE_PUBLIC_METHODS
                ));

        // 注册资源提示
        hints.resources().registerPattern("META-INF/spring/*");
    }
}
