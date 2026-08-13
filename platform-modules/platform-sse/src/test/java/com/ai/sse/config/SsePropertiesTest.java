package com.ai.sse.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SsePropertiesTest {

    @Test
    void ticketTtlShouldDefaultToShortLivedValue() {
        SseProperties properties = properties(null, null, null);

        assertThat(properties.ticketTtl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.pushPreferenceQueryTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.pushPreferenceFailurePolicy())
                .isEqualTo(SseProperties.PushPreferenceFailurePolicy.FAIL_CLOSED);
    }

    @Test
    void ticketTtlShouldRejectUnsafeDuration() {
        assertThatThrownBy(() -> properties(Duration.ZERO, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SSE 票据有效期必须大于 0 且不超过 5 分钟");
        assertThatThrownBy(() -> properties(Duration.ofMinutes(6), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pushPreferenceQueryTimeoutShouldRejectUnsafeDuration() {
        assertThatThrownBy(() -> properties(null, Duration.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("推送偏好查询超时时间必须大于 0 且不超过 30 秒");
        assertThatThrownBy(() -> properties(null, Duration.ofSeconds(31), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pushPreferenceFailurePolicyShouldAcceptExplicitRetry() {
        SseProperties properties = properties(
                null, Duration.ofMillis(500), SseProperties.PushPreferenceFailurePolicy.RETRY);

        assertThat(properties.pushPreferenceQueryTimeout()).isEqualTo(Duration.ofMillis(500));
        assertThat(properties.pushPreferenceFailurePolicy())
                .isEqualTo(SseProperties.PushPreferenceFailurePolicy.RETRY);
    }

    @Test
    void connectionPolicyShouldRejectUnsafeLimits() {
        assertThatThrownBy(() -> new SseProperties(
                0L, null, null, null, null, null, null, null, null, null,
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SSE 连接超时时间");
        assertThatThrownBy(() -> new SseProperties(
                null, null, 1_000_001, null, null, null, null, null, null, null,
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SSE 单节点最大连接数");
        assertThatThrownBy(() -> new SseProperties(
                null, null, null, null, null, null, null, null, null, " ",
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Redis PubSub 通道名");
    }

    private SseProperties properties(
            Duration ticketTtl,
            Duration pushPreferenceQueryTimeout,
            SseProperties.PushPreferenceFailurePolicy failurePolicy) {
        return new SseProperties(
                null, null, null, null, null, null, null, null, null, null,
                ticketTtl, pushPreferenceQueryTimeout, failurePolicy, null);
    }
}
