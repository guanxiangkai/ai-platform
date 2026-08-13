package com.ai.sse.consumer;

import io.github.guanxiangkai.web.plus.mq.model.MqMessage;
import io.github.guanxiangkai.web.plus.mq.model.SsePushLogMessage;
import com.ai.sse.domain.entity.SsePushRecord;
import com.ai.sse.service.ISsePushRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * SSE 推送结果消费者配置。
 *
 * <p>消费 {@code log.sse-push} 主题，将每次推送结果持久化为
 * {@link SsePushRecord}。基于 {@code @SseLog} 的接口操作审计由
 * {@link com.ai.sse.log.SseLogHandlerImpl} 负责。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SsePushLogConsumerConfig {

    private final ISsePushRecordService pushRecordService;
    private final ObjectMapper objectMapper;

    /**
     * SSE 推送日志消费者
     * Binding：ssePushLogConsumer-in-0 → topic: log.sse-push
     */
    @Bean
    public Consumer<Message<MqMessage>> ssePushLogConsumer() {
        return message -> {
            MqMessage raw = message.getPayload();
            if (raw.payload() == null) {
                log.warn("ssePushLogConsumer 收到空 payload，跳过处理");
                return;
            }
            SsePushLogMessage record = objectMapper.convertValue(raw.payload(), SsePushLogMessage.class);
            if (record == null) {
                log.warn("ssePushLogConsumer payload 反序列化结果为 null，跳过处理");
                return;
            }
            try {
                SsePushRecord entity = new SsePushRecord();
                entity.setMessageId(record.messageId());
                entity.setTargetType(record.targetType());
                entity.setMessageType(record.messageType());
                entity.setUserId(record.userId());
                entity.setUserIds(record.userIds());
                entity.setPushContent(record.content());
                entity.setPushStatus(record.pushStatus());
                entity.setFailReason(record.failReason());
                entity.setRetryCount(record.retryCount());
                entity.setPushTime(record.pushTime() != null ? record.pushTime() : LocalDateTime.now());
                if (record.tenantId() != null) entity.setTenantId(record.tenantId());
                pushRecordService.createEntity(entity);
                log.info("SSE推送日志保存成功: messageId={}, status={}", record.messageId(), record.pushStatus());
            } catch (Exception e) {
                log.error("保存SSE推送日志失败: messageId={}", record.messageId(), e);
                throw new RuntimeException("保存SSE推送日志失败", e);
            }
        };
    }
}
