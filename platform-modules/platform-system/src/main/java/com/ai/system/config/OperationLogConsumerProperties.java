package com.ai.system.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 操作日志 Redis Stream 消费与待确认记录恢复策略。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "platform.system.operation-log-consumer")
public class OperationLogConsumerProperties {

    /** 每轮恢复的待确认记录数量上限。 */
    @Min(1)
    @Max(1_000)
    private int pendingBatchSize = 100;

    /** Redis Stream 阻塞轮询周期。 */
    @NotNull
    private Duration pollTimeout = Duration.ofSeconds(2);

    /** 待确认记录恢复周期。 */
    @NotNull
    private Duration pendingRecoveryDelay = Duration.ofSeconds(30);

    /** 校验轮询周期处于安全范围。 */
    @AssertTrue(message = "操作日志轮询周期必须大于 0 且不超过 1 分钟")
    public boolean isPollTimeoutWithinBounds() {
        return pollTimeout != null && !pollTimeout.isZero() && !pollTimeout.isNegative()
                && pollTimeout.compareTo(Duration.ofMinutes(1)) <= 0;
    }

    /** 校验待确认记录恢复周期处于安全范围。 */
    @AssertTrue(message = "操作日志恢复周期必须大于 0 且不超过 1 小时")
    public boolean isPendingRecoveryDelayWithinBounds() {
        return pendingRecoveryDelay != null
                && !pendingRecoveryDelay.isZero()
                && !pendingRecoveryDelay.isNegative()
                && pendingRecoveryDelay.compareTo(Duration.ofHours(1)) <= 0;
    }
}
