package com.ai.system.consumer.log;

import com.ai.api.context.TenantExecutionScope;
import com.ai.api.system.log.OperationLogStreamCodec;
import com.ai.system.config.OperationLogConsumerProperties;
import com.ai.system.domain.entity.OperationLog;
import com.ai.system.service.IOperationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisOperationLogConsumerConfigTest {

    @Test
    void startShouldRegisterSubscriptionWhenContainerIsAlreadyRunning() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<IOperationLogService> serviceProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                mock(StreamMessageListenerContainer.class);
        RedisOperationLogConsumerConfig consumer = new RedisOperationLogConsumerConfig(
                redisTemplate,
                mock(RedisConnectionFactory.class),
                serviceProvider,
                new OperationLogConsumerProperties()
        );

        when(redisTemplate.hasKey(OperationLogStreamCodec.STREAM_KEY)).thenReturn(true);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(container.isRunning()).thenReturn(true);
        ReflectionTestUtils.setField(consumer, "container", container);

        consumer.start();

        verify(container).receive(any(Consumer.class), any(StreamOffset.class), any());
        verify(container, never()).start();
    }

    @Test
    void consumeShouldPersistInsideRecordTenantScope() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<IOperationLogService> serviceProvider = mock(ObjectProvider.class);
        IOperationLogService service = mock(IOperationLogService.class);
        RedisOperationLogConsumerConfig consumer = new RedisOperationLogConsumerConfig(
                redisTemplate,
                mock(RedisConnectionFactory.class),
                serviceProvider,
                new OperationLogConsumerProperties()
        );
        MapRecord<String, String, String> record = MapRecord.create(
                OperationLogStreamCodec.STREAM_KEY,
                Map.of(
                        "tenantId", "tenant-1",
                        "operationId", "operation-1",
                        "module", "System",
                        "username", "顾登科",
                        "status", "SUCCESS",
                        "logTime", "2026-08-06T10:00:00"
                )
        );
        AtomicReference<String> tenantIdDuringSave = new AtomicReference<>();
        AtomicReference<OperationLog> savedLog = new AtomicReference<>();

        when(serviceProvider.getIfAvailable()).thenReturn(service);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        doAnswer(invocation -> {
            tenantIdDuringSave.set(TenantExecutionScope.currentTenantId());
            savedLog.set(invocation.getArgument(0));
            return null;
        }).when(service).createEntity(any(OperationLog.class));

        consumer.consume(record);

        assertThat(tenantIdDuringSave).hasValue("tenant-1");
        assertThat(savedLog.get().getTenantId()).isEqualTo("tenant-1");
        assertThat(savedLog.get().getOperationId()).isEqualTo("operation-1");
        assertThat(savedLog.get().getStatus()).isEqualTo("SUCCESS");
        assertThat(TenantExecutionScope.currentTenantId()).isNull();
        verify(streamOperations).acknowledge(
                OperationLogStreamCodec.STREAM_KEY,
                OperationLogStreamCodec.CONSUMER_GROUP,
                record.getId()
        );
    }

    @Test
    void consumeShouldAcknowledgeRecordWithoutRequiredFields() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<IOperationLogService> serviceProvider = mock(ObjectProvider.class);
        IOperationLogService service = mock(IOperationLogService.class);
        RedisOperationLogConsumerConfig consumer = new RedisOperationLogConsumerConfig(
                redisTemplate,
                mock(RedisConnectionFactory.class),
                serviceProvider,
                new OperationLogConsumerProperties()
        );
        MapRecord<String, String, String> record = MapRecord.create(
                OperationLogStreamCodec.STREAM_KEY,
                Map.of("module", "System", "username", "顾登科")
        );

        when(serviceProvider.getIfAvailable()).thenReturn(service);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        consumer.consume(record);

        verify(service, never()).createEntity(any(OperationLog.class));
        verify(streamOperations).acknowledge(
                OperationLogStreamCodec.STREAM_KEY,
                OperationLogStreamCodec.CONSUMER_GROUP,
                record.getId()
        );
    }

    @Test
    void consumeShouldKeepRecordPendingWhenPersistenceFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<IOperationLogService> serviceProvider = mock(ObjectProvider.class);
        IOperationLogService service = mock(IOperationLogService.class);
        RedisOperationLogConsumerConfig consumer = new RedisOperationLogConsumerConfig(
                redisTemplate,
                mock(RedisConnectionFactory.class),
                serviceProvider,
                new OperationLogConsumerProperties()
        );
        MapRecord<String, String, String> record = MapRecord.create(
                OperationLogStreamCodec.STREAM_KEY,
                Map.of(
                        "tenantId", "tenant-1",
                        "operationId", "operation-1",
                        "status", "SUCCESS",
                        "logTime", "2026-08-09T10:00:00"
                )
        );

        when(serviceProvider.getIfAvailable()).thenReturn(service);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        doThrow(new IllegalStateException("数据库暂不可用")).when(service).createEntity(any(OperationLog.class));

        consumer.consume(record);

        verify(streamOperations, never()).acknowledge(
                OperationLogStreamCodec.STREAM_KEY,
                OperationLogStreamCodec.CONSUMER_GROUP,
                record.getId()
        );
    }
}
