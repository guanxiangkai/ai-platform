package com.ai.system.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 用户授权范围缓存策略。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "platform.system.authorization-cache")
public class AuthorizationCacheProperties {

    /** 授权范围缓存有效期。 */
    @NotNull
    private Duration ttl = Duration.ofMinutes(5);

    /** 校验缓存有效期处于可运维的安全范围。 */
    @AssertTrue(message = "授权范围缓存有效期必须大于 0 且不超过 1 天")
    public boolean isTtlWithinBounds() {
        return ttl != null && !ttl.isZero() && !ttl.isNegative()
                && ttl.compareTo(Duration.ofDays(1)) <= 0;
    }
}
