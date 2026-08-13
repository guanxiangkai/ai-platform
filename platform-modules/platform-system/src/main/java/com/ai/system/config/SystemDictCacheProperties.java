package com.ai.system.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 系统字典三级缓存配置。
 *
 * <p>沿用 jpa-plus.dict.cache.ttl-seconds，避免自定义 DictProvider 与
 * JPA Plus 字典缓存配置分叉。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
@Validated
@ConfigurationProperties(prefix = "jpa-plus.dict.cache")
public class SystemDictCacheProperties {

    @Min(1)
    private long ttlSeconds = 3600L;

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds <= 0) {
            throw new IllegalArgumentException("系统字典缓存有效期必须大于 0 秒");
        }
        this.ttlSeconds = ttlSeconds;
    }

    public Duration ttl() {
        return Duration.ofSeconds(ttlSeconds);
    }
}
