package com.ai.system.consumer.log;

import com.ai.api.context.TenantExecutionScope;
import com.ai.api.system.log.OperationLogStreamCodec;
import com.ai.api.system.log.OperationLogStreamCodec.OperationLogStreamRecord;
import com.ai.system.config.OperationLogConsumerProperties;
import com.ai.system.domain.entity.OperationLog;
import com.ai.system.service.IOperationLogService;
import io.lettuce.core.RedisBusyException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 将共享 Redis Stream 中的操作日志持久化到系统数据库。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(OperationLogConsumerProperties.class)
public class RedisOperationLogConsumerConfig {

    private final StringRedisTemplate redisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ObjectProvider<IOperationLogService> operationLogServiceProvider;
    private final OperationLogConsumerProperties properties;
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private ScheduledExecutorService pendingRecoveryExecutor;

    public RedisOperationLogConsumerConfig(
            @Qualifier("authStringRedisTemplate") StringRedisTemplate redisTemplate,
            @Qualifier("authRedisConnectionFactory") RedisConnectionFactory redisConnectionFactory,
            ObjectProvider<IOperationLogService> operationLogServiceProvider,
            OperationLogConsumerProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.operationLogServiceProvider = operationLogServiceProvider;
        this.properties = properties;
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> operationLogStreamContainer() {
        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .<String, MapRecord<String, String, String>>builder()
                .pollTimeout(properties.getPollTimeout())
                .build();
        container = StreamMessageListenerContainer.create(redisConnectionFactory, options);
        return container;
    }

    /**
     * 应用上下文完全就绪后再启动消费，避免监听线程在 Bean 创建阶段反向触发尚未完成的配置绑定。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (container == null) {
            return;
        }
        ensureGroup();
        container.receive(
                Consumer.from(OperationLogStreamCodec.CONSUMER_GROUP, OperationLogStreamCodec.CONSUMER_NAME),
                StreamOffset.create(OperationLogStreamCodec.STREAM_KEY, ReadOffset.lastConsumed()),
                this::consume
        );
        if (!container.isRunning()) {
            container.start();
        }
        startPendingRecovery();
    }

    @PreDestroy
    public void stop() {
        if (container != null) {
            container.stop();
        }
        if (pendingRecoveryExecutor != null) {
            pendingRecoveryExecutor.shutdownNow();
        }
    }

    private void ensureGroup() {
        try {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(OperationLogStreamCodec.STREAM_KEY))) {
                redisTemplate.opsForStream().add(
                        OperationLogStreamCodec.STREAM_KEY,
                        Map.of(
                                OperationLogStreamCodec.INITIALIZATION_FIELD,
                                OperationLogStreamCodec.INITIALIZATION_VALUE
                        )
                );
            }
            redisTemplate.opsForStream().createGroup(
                    OperationLogStreamCodec.STREAM_KEY,
                    ReadOffset.from("0-0"),
                    OperationLogStreamCodec.CONSUMER_GROUP
            );
        } catch (RedisSystemException e) {
            if (!isBusyGroup(e)) {
                throw e;
            }
        }
    }

    void consume(MapRecord<String, String, String> record) {
        Map<String, String> values = record.getValue();
        if (OperationLogStreamCodec.isInitializationRecord(values)) {
            acknowledge(record);
            return;
        }
        OperationLogStreamRecord streamRecord;
        try {
            streamRecord = OperationLogStreamCodec.decode(values);
        } catch (IllegalArgumentException exception) {
            acknowledgeInvalidRecord(record, exception);
            return;
        }
        try {
            IOperationLogService service = operationLogServiceProvider.getIfAvailable();
            if (service == null) {
                throw new IllegalStateException("操作日志服务未初始化");
            }
            TenantExecutionScope.run(streamRecord.tenantId(), () -> service.createEntity(toEntity(streamRecord)));
            acknowledge(record);
        } catch (Exception e) {
            log.error("[RedisOperationLogConsumer] 保存操作日志失败，记录保留在待确认队列: module={}, exception={}",
                    values.get("module"), e.getClass().getSimpleName());
        }
    }

    /**
     * 确认无法满足当前 Stream 契约的记录，避免其在待确认队列中被无限重放。
     *
     * <p>字段校验失败的记录不可能由当前协议恢复为有效操作日志；持久化等瞬态异常仍保留在待确认队列中重试。</p>
     *
     * @param record 不符合当前协议的 Stream 记录
     * @param exception 字段校验失败原因
     */
    private void acknowledgeInvalidRecord(MapRecord<String, String, String> record, IllegalArgumentException exception) {
        log.warn("[RedisOperationLogConsumer] 丢弃不符合当前操作日志 Stream 契约的记录: id={}, exception={}",
                record.getId(), exception.getClass().getSimpleName());
        acknowledge(record);
    }

    private OperationLog toEntity(OperationLogStreamRecord values) {
        OperationLog entity = new OperationLog();
        entity.setTraceId(values.traceId());
        entity.setUserId(values.userId());
        entity.setUsername(values.username());
        entity.setClientIp(values.clientIp());
        entity.setLocation(values.location());
        entity.setStatus(values.status());
        entity.setMessage(values.message());
        entity.setLogTime(values.logTime());
        entity.setTenantId(values.tenantId());
        entity.setOperationId(values.operationId());
        entity.setModule(values.module());
        entity.setOperationTypeCode(values.operationTypeCode());
        entity.setDescription(values.description());
        entity.setRequestMethod(values.requestMethod());
        entity.setRequestUrl(values.requestUrl());
        entity.setRequestParams(values.requestParams());
        entity.setResponseData(values.responseData());
        entity.setUserAgent(values.userAgent());
        entity.setCostMs(values.costMs());
        entity.setErrorMessage(values.errorMessage());
        return entity;
    }

    private void acknowledge(MapRecord<String, String, String> record) {
        redisTemplate.opsForStream().acknowledge(
                OperationLogStreamCodec.STREAM_KEY,
                OperationLogStreamCodec.CONSUMER_GROUP,
                record.getId()
        );
    }

    private void startPendingRecovery() {
        if (pendingRecoveryExecutor != null) {
            return;
        }
        pendingRecoveryExecutor = Executors.newSingleThreadScheduledExecutor(task ->
                Thread.ofPlatform()
                        .daemon(true)
                        .name("operation-log-pending-recovery")
                        .unstarted(task));
        pendingRecoveryExecutor.scheduleWithFixedDelay(
                this::recoverPendingRecords,
                properties.getPendingRecoveryDelay().toMillis(),
                properties.getPendingRecoveryDelay().toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /** 重试当前消费者尚未确认的操作日志，服务重启后也会继续处理。 */
    void recoverPendingRecords() {
        try {
            StreamOperations<String, String, String> streamOperations = redisTemplate.opsForStream();
            StreamOffset<String> pendingOffset = StreamOffset.create(
                    OperationLogStreamCodec.STREAM_KEY,
                    ReadOffset.from("0")
            );
            @SuppressWarnings("unchecked")
            List<MapRecord<String, String, String>> pending = streamOperations.read(
                    Consumer.from(OperationLogStreamCodec.CONSUMER_GROUP, OperationLogStreamCodec.CONSUMER_NAME),
                    StreamReadOptions.empty().count(properties.getPendingBatchSize()),
                    pendingOffset
            );
            if (pending == null) {
                return;
            }
            for (MapRecord<String, String, String> record : pending) {
                consume(record);
            }
        } catch (Exception exception) {
            log.error("[RedisOperationLogConsumer] 重试待确认操作日志失败: exception={}",
                    exception.getClass().getSimpleName());
        }
    }

    private boolean isBusyGroup(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof RedisBusyException
                    || (current.getMessage() != null && current.getMessage().contains("BUSYGROUP"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
