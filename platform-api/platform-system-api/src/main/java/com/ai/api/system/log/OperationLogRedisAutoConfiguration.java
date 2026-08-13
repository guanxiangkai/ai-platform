package com.ai.api.system.log;

import io.github.guanxiangkai.web.plus.log.spi.OperationLogHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 将各租户产品产生的操作日志统一发布到平台 Redis Stream。
 *
 * <p>产品服务只负责发布带租户标识的日志；{@code platform-system} 是日志表的唯一写入方。
 * 该自动配置复用 Web Plus 创建的认证 Redis 连接，避免每个产品维护一套消息绑定和日志模块。</p>
 */
@AutoConfiguration
@ConditionalOnClass({OperationLogHandler.class, StringRedisTemplate.class})
@ConditionalOnBean(name = "authStringRedisTemplate")
@ConditionalOnProperty(prefix = "web-plus.log", name = "operation-log-enabled", havingValue = "true", matchIfMissing = true)
public class OperationLogRedisAutoConfiguration {

    /**
     * 创建统一的操作日志发布器。
     *
     * @param redisTemplate 认证 Redis 模板，固定连接共享认证数据库
     * @return Web Plus 操作日志处理器
     */
    @Bean
    @ConditionalOnMissingBean(OperationLogHandler.class)
    public OperationLogHandler platformOperationLogHandler(
            @Qualifier("authStringRedisTemplate") StringRedisTemplate redisTemplate
    ) {
        return entity -> {
            if (!(entity instanceof PlatformOperationLog operationLog)) {
                throw new IllegalArgumentException("操作日志必须使用 PlatformOperationLog 当前契约");
            }
            RecordId recordId = redisTemplate.opsForStream()
                    .add(MapRecord.create(
                            OperationLogStreamCodec.STREAM_KEY,
                            OperationLogStreamCodec.encode(operationLog)
                    ));
            if (recordId == null) {
                throw new IllegalStateException("操作日志未写入平台 Redis Stream");
            }
        };
    }
}
