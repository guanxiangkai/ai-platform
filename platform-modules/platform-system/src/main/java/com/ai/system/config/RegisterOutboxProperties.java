package com.ai.system.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 注册可靠出站任务的受校验运行参数。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "platform.system.register-outbox")
public class RegisterOutboxProperties {
    private Duration interval = Duration.ofSeconds(5);
    @Min(1) @Max(200) private int batchSize = 20;
    private Duration retryDelay = Duration.ofMinutes(1);
    private Duration processingLease = Duration.ofMinutes(2);
    @Min(1) @Max(100) private int maxAttempts = 10;

    @AssertTrue(message = "注册 outbox 的时间参数必须为正且不超过一小时")
    public boolean hasValidDurations() {
        return isValid(interval) && isValid(retryDelay) && isValid(processingLease);
    }

    private boolean isValid(Duration value) {
        return value != null && !value.isNegative() && !value.isZero() && value.compareTo(Duration.ofHours(1)) <= 0;
    }
}
