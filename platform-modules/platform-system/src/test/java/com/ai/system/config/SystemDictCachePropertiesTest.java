package com.ai.system.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 系统字典缓存运行参数测试。 */
class SystemDictCachePropertiesTest {

    @Test
    void shouldProvideCurrentDefaultAndAcceptPositiveTtl() {
        SystemDictCacheProperties properties = new SystemDictCacheProperties();
        assertThat(properties.ttl()).isEqualTo(Duration.ofHours(1));

        properties.setTtlSeconds(300L);
        assertThat(properties.ttl()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldRejectMissingOrNonPositiveTtl() {
        SystemDictCacheProperties properties = new SystemDictCacheProperties();

        assertThatThrownBy(() -> properties.setTtlSeconds(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties.setTtlSeconds(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("系统字典缓存有效期必须大于 0 秒");
    }
}
