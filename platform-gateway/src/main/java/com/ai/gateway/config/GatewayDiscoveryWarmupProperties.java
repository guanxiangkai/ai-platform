package com.ai.gateway.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 网关服务发现预热参数。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "ai.gateway.discovery-warmup")
public class GatewayDiscoveryWarmupProperties {

    /** 单个服务完成首次发现查询的最长时间。 */
    @NotNull
    private Duration timeout = Duration.ofSeconds(15);

    /** 同时预热的最大服务数。 */
    @Min(1)
    @Max(16)
    private int concurrency = 4;

    /** 校验发现查询超时处于安全范围。 */
    @AssertTrue(message = "网关服务发现预热超时必须大于 0 且不超过 60 秒")
    public boolean isTimeoutWithinBounds() {
        return timeout != null
                && !timeout.isZero()
                && !timeout.isNegative()
                && timeout.compareTo(Duration.ofSeconds(60)) <= 0;
    }
}
