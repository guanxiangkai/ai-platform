package com.ai.sse.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * SSE 配置属性
 *
 * @param timeout               连接超时时间（毫秒），默认 30 分钟
 * @param heartbeatInterval     心跳间隔（毫秒），默认 30 秒
 * @param maxConnections        单节点最大连接数，默认 10000
 * @param maxConnectionsPerUser 单用户最大并发连接数（多标签页/设备），默认 3
 * @param cleanupInterval       过期连接清理间隔（毫秒），默认 60 秒
 * @param ticketRateLimit       /sse/ticket 每窗口最大请求数（每用户），默认 5
 * @param ticketRateWindow      /sse/ticket 限流窗口（毫秒），默认 60 秒
 * @param ticketBindContext     是否绑定 ticket 到客户端上下文（IP + UA），默认 true
 * @param enablePubsub          是否启用 Redis PubSub 多实例路由，默认 true
 * @param pubsubChannel         Redis PubSub 通道名，默认 "sse:push"
 * @param ticketTtl             一次性连接票据有效期，默认 30 秒
 * @param pushPreferenceQueryTimeout 用户推送偏好批量查询超时时间，默认 2 秒
 * @param pushPreferenceFailurePolicy 用户推送偏好查询失败策略，默认关闭推送
 * @param historyCleanupBatchSize 单次历史清理的最大记录数，默认 500
 * @author guanxiangkai
 * @since 1.0.0
 */
@RegisterReflectionForBinding
@Validated
@ConfigurationProperties(prefix = "ai.sse")
public record SseProperties(
        @Min(1) @Max(86_400_000) Long timeout,
        @Min(1) @Max(300_000) Long heartbeatInterval,
        @Min(1) @Max(1_000_000) Integer maxConnections,
        @Min(1) @Max(100) Integer maxConnectionsPerUser,
        @Min(1) @Max(3_600_000) Long cleanupInterval,
        @Min(1) @Max(10_000) Integer ticketRateLimit,
        @Min(1) @Max(3_600_000) Long ticketRateWindow,
        Boolean ticketBindContext,
        Boolean enablePubsub,
        @NotBlank @Size(max = 128) String pubsubChannel,
        Duration ticketTtl,
        @NotNull Duration pushPreferenceQueryTimeout,
        @NotNull PushPreferenceFailurePolicy pushPreferenceFailurePolicy,
        @Min(1) Integer historyCleanupBatchSize
) {

    /**
     * 紧凑型构造器 — 填充默认值
     */
    public SseProperties {
        if (timeout == null) {
            timeout = 1_800_000L;
        }
        if (heartbeatInterval == null) {
            heartbeatInterval = 30_000L;
        }
        if (maxConnections == null) {
            maxConnections = 10_000;
        }
        if (maxConnectionsPerUser == null) {
            maxConnectionsPerUser = 3;
        }
        if (cleanupInterval == null) {
            cleanupInterval = 60_000L;
        }
        if (ticketRateLimit == null) {
            ticketRateLimit = 5;
        }
        if (ticketRateWindow == null) {
            ticketRateWindow = 60_000L;
        }
        if (ticketBindContext == null) {
            ticketBindContext = true;
        }
        if (enablePubsub == null) {
            enablePubsub = true;
        }
        if (pubsubChannel == null) {
            pubsubChannel = "sse:push";
        }
        if (ticketTtl == null) {
            ticketTtl = Duration.ofSeconds(30);
        }
        if (ticketTtl.isZero() || ticketTtl.isNegative() || ticketTtl.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("SSE 票据有效期必须大于 0 且不超过 5 分钟");
        }
        if (pushPreferenceQueryTimeout == null) {
            pushPreferenceQueryTimeout = Duration.ofSeconds(2);
        }
        if (pushPreferenceQueryTimeout.isZero() || pushPreferenceQueryTimeout.isNegative()
                || pushPreferenceQueryTimeout.compareTo(Duration.ofSeconds(30)) > 0) {
            throw new IllegalArgumentException("推送偏好查询超时时间必须大于 0 且不超过 30 秒");
        }
        if (pushPreferenceFailurePolicy == null) {
            pushPreferenceFailurePolicy = PushPreferenceFailurePolicy.FAIL_CLOSED;
        }
        if (historyCleanupBatchSize == null) {
            historyCleanupBatchSize = 500;
        }
        requireRange(timeout, 1, 86_400_000, "SSE 连接超时时间");
        requireRange(heartbeatInterval, 1, 300_000, "SSE 心跳间隔");
        requireRange(maxConnections, 1, 1_000_000, "SSE 单节点最大连接数");
        requireRange(maxConnectionsPerUser, 1, 100, "SSE 单用户最大连接数");
        requireRange(cleanupInterval, 1, 3_600_000, "SSE 清理间隔");
        requireRange(ticketRateLimit, 1, 10_000, "SSE 票据窗口请求上限");
        requireRange(ticketRateWindow, 1, 3_600_000, "SSE 票据限流窗口");
        if (pubsubChannel.isBlank() || pubsubChannel.length() > 128) {
            throw new IllegalArgumentException("SSE Redis PubSub 通道名不能为空且不能超过 128 个字符");
        }
        if (historyCleanupBatchSize < 1 || historyCleanupBatchSize > 10_000) {
            throw new IllegalArgumentException("SSE 历史清理单次记录数必须介于 1 到 10000");
        }
    }

    private static void requireRange(long value, long minimum, long maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + "必须介于 " + minimum + " 到 " + maximum);
        }
    }

    /** 用户推送偏好批量查询失败时的处理策略。 */
    public enum PushPreferenceFailurePolicy {
        /** 查询失败时跳过推送，避免绕过用户偏好。 */
        FAIL_CLOSED,
        /** 查询失败时抛出异常，由消息中间件的重试与死信策略接管。 */
        RETRY
    }
}
