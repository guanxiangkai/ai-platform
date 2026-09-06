package com.ai.sse.core;

import com.ai.sse.constants.SseConstants;
import com.ai.sse.event.SseConnectEvent;
import com.ai.sse.event.SseDisconnectEvent;
import com.ai.sse.event.SseMessageSentEvent;
import com.ai.sse.model.SseConnection;
import com.ai.sse.model.SseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSE 操作默认实现（WebFlux 响应式版本）
 * <p>
 * 设计模式：
 * <ul>
 *   <li><b>观察者模式</b> — 连接/断开/消息事件通过 Spring {@link ApplicationEventPublisher} 发布</li>
 *   <li><b>模板方法</b> — {@link #doDisconnectOne} 统一断开单个连接逻辑</li>
 * </ul>
 * </p>
 * <p>
 * 多连接支持：同一用户可通过多标签页/多设备建立多个 SSE 连接，
 * 每个连接有独立的 {@code connectionId}，受 {@code maxConnectionsPerUser} 限制。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
public class DefaultSseOperations implements SseOperations {

    /**
     * userId → 该用户的所有连接列表
     */
    private final Map<String, CopyOnWriteArrayList<SseConnection>> connections = new ConcurrentHashMap<>();

    private final long timeout;
    private final int maxConnections;
    private final int maxConnectionsPerUser;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== 运维指标 ====================

    private final AtomicLong totalConnections = new AtomicLong();
    private final AtomicLong totalMessagesSent = new AtomicLong();
    private final AtomicLong totalMessagesFailed = new AtomicLong();

    public DefaultSseOperations(long timeout, int maxConnections, int maxConnectionsPerUser,
                                ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.timeout = timeout;
        this.maxConnections = maxConnections;
        this.maxConnectionsPerUser = maxConnectionsPerUser;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Flux<ServerSentEvent<String>> connect(String userId, String tenantId) {
        SseConnection connection;
        Sinks.Many<String> sink;
        int userConnectionCount;
        synchronized (connections) {
            if (getTotalConnectionCount() >= maxConnections) {
                log.warn("SSE 全局连接数已达上限: max={}, userId={}", maxConnections, userId);
                return Flux.just(ServerSentEvent.<String>builder()
                        .data("{\"error\":\"max_connections_reached\"}")
                        .build());
            }
            CopyOnWriteArrayList<SseConnection> userConns = connections.computeIfAbsent(userId,
                    _ -> new CopyOnWriteArrayList<>());
            if (userConns.size() >= maxConnectionsPerUser) {
                SseConnection oldest = userConns.getFirst();
                log.info("SSE 用户连接数达上限，踢掉最早连接: userId={}, connId={}, max={}",
                        userId, oldest.connectionId(), maxConnectionsPerUser);
                doDisconnectOne(userId, oldest, SseConstants.ConnectionStatus.DISCONNECTED);
            }
            sink = Sinks.many().unicast().onBackpressureBuffer();
            connection = SseConnection.of(userId, sink, tenantId);
            userConns.add(connection);
            totalConnections.incrementAndGet();
            userConnectionCount = userConns.size();
        }

        // 发送连接成功消息
        sendConnectMessage(userId, connection.connectionId(), sink);

        // 发布连接事件
        eventPublisher.publishEvent(new SseConnectEvent(this, connection));
        log.info("SSE 连接建立: userId={}, connId={}, tenantId={}, userConns={}, totalOnline={}",
                userId, connection.connectionId(), tenantId, userConnectionCount, getTotalConnectionCount());

        return sink.asFlux()
                .map(data -> ServerSentEvent.<String>builder().data(data).build())
                .doOnCancel(() -> doDisconnectOne(userId, connection, SseConstants.ConnectionStatus.DISCONNECTED))
                .doOnTerminate(() -> doDisconnectOne(userId, connection, SseConstants.ConnectionStatus.DISCONNECTED));
    }

    @Override
    public void disconnect(String userId) {
        CopyOnWriteArrayList<SseConnection> userConns = connections.remove(userId);
        if (userConns == null || userConns.isEmpty()) return;
        for (SseConnection conn : userConns) {
            closeSink(conn);
            eventPublisher.publishEvent(new SseDisconnectEvent(this, conn.withStatus(SseConstants.ConnectionStatus.DISCONNECTED)));
        }
        log.info("SSE 用户所有连接断开: userId={}, count={}", userId, userConns.size());
    }

    @Override
    public Optional<SseConnection> getConnection(String userId) {
        CopyOnWriteArrayList<SseConnection> userConns = connections.get(userId);
        if (userConns == null) return Optional.empty();
        // Take a snapshot to avoid TOCTOU: the list may become empty between isEmpty() and getFirst()
        List<SseConnection> snapshot = List.copyOf(userConns);
        if (snapshot.isEmpty()) return Optional.empty();
        return Optional.of(snapshot.get(0));
    }

    @Override
    public boolean isOnline(String userId) {
        CopyOnWriteArrayList<SseConnection> userConns = connections.get(userId);
        return userConns != null && !userConns.isEmpty();
    }

    @Override
    public int getOnlineCount() {
        return (int) connections.values().stream()
                .filter(list -> !list.isEmpty())
                .count();
    }

    @Override
    public int getOnlineCountByTenant(String tenantId) {
        if (tenantId == null) return 0;
        return (int) connections.values().stream()
                .filter(list -> !list.isEmpty())
                .filter(list -> list.stream().anyMatch(c -> tenantId.equals(c.tenantId())))
                .count();
    }

    @Override
    public void sendToUser(String userId, SseMessage<?> message) {
        CopyOnWriteArrayList<SseConnection> userConns = connections.get(userId);
        if (userConns == null || userConns.isEmpty()) {
            log.debug("用户不在线: userId={}", userId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            List<SseConnection> failedConns = new ArrayList<>();

            for (SseConnection conn : userConns) {
                if (conn.sink() == null) {
                    failedConns.add(conn);
                    continue;
                }
                Sinks.EmitResult result = conn.sink().tryEmitNext(json);
                if (result.isFailure()) {
                    log.warn("发送消息失败: userId={}, connId={}, result={}", userId, conn.connectionId(), result);
                    totalMessagesFailed.incrementAndGet();
                    failedConns.add(conn);
                } else {
                    // 更新活跃时间（替换列表中的旧对象）
                    int idx = userConns.indexOf(conn);
                    if (idx >= 0) {
                        userConns.set(idx, conn.updateActiveTime());
                    }
                }
            }

            // 清理发送失败的连接
            for (SseConnection failed : failedConns) {
                doDisconnectOne(userId, failed, SseConstants.ConnectionStatus.ERROR);
            }

            int successCount = userConns.size() - failedConns.size();
            if (successCount > 0) {
                totalMessagesSent.incrementAndGet();
                eventPublisher.publishEvent(new SseMessageSentEvent(this, message));
            }
            log.debug("发送消息: userId={}, type={}, activeConns={}", userId, message.type(), successCount);
        } catch (Exception e) {
            log.error("发送消息异常: userId={}", userId, e);
            totalMessagesFailed.incrementAndGet();
        }
    }

    @Override
    public void sendToUsers(List<String> userIds, SseMessage<?> message) {
        if (userIds == null || userIds.isEmpty()) return;
        userIds.forEach(userId -> sendToUser(userId, message));
    }

    @Override
    public void broadcast(SseMessage<?> message) {
        log.info("广播消息: onlineUsers={}, type={}", connections.size(), message.type());
        connections.keySet().forEach(userId -> sendToUser(userId, message));
    }

    @Override
    public void broadcastToTenant(String tenantId, SseMessage<?> message) {
        if (tenantId == null) return;
        List<String> tenantUsers = connections.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .filter(e -> e.getValue().stream().anyMatch(c -> tenantId.equals(c.tenantId())))
                .map(Map.Entry::getKey)
                .toList();
        log.info("租户广播: tenantId={}, userCount={}", tenantId, tenantUsers.size());
        sendToUsers(tenantUsers, message);
    }

    @Override
    public void sendToGroup(String groupId, List<String> userIds, SseMessage<?> message) {
        if (groupId == null || userIds == null || userIds.isEmpty()) return;
        SseMessage<?> groupMessage = SseMessage.builder()
                .id(message.id())
                .type(message.type())
                .senderId(message.senderId())
                .senderName(message.senderName())
                .groupId(groupId)
                .content(message.content())
                .sendTime(message.sendTime())
                .tenantId(message.tenantId())
                .build();
        log.info("群组消息: groupId={}, memberCount={}", groupId, userIds.size());
        sendToUsers(userIds, groupMessage);
    }

    // ==================== 服务端心跳 ====================

    /**
     * 向所有在线连接发送心跳（由定时任务调用）
     * <p>
     * 发送 heartbeat 消息作为 keepalive，防止代理/防火墙断开连接。
     * 心跳成功会更新 {@code lastActiveTime}，失败则断开连接。
     * </p>
     *
     * @return 成功发送心跳的连接数
     */
    @Override
    public int sendHeartbeatToAll() {
        int successCount = 0;
        List<Map.Entry<String, SseConnection>> failedEntries = new ArrayList<>();

        for (var entry : connections.entrySet()) {
            String userId = entry.getKey();
            CopyOnWriteArrayList<SseConnection> userConns = entry.getValue();

            for (SseConnection conn : userConns) {
                if (conn.sink() == null) {
                    failedEntries.add(Map.entry(userId, conn));
                    continue;
                }
                try {
                    // 发送心跳消息（轻量级 ":" 注释或 heartbeat 消息）
                    SseMessage<String> heartbeat = SseMessage.heartbeat(userId);
                    String json = objectMapper.writeValueAsString(heartbeat);
                    Sinks.EmitResult result = conn.sink().tryEmitNext(json);

                    if (result.isSuccess()) {
                        int idx = userConns.indexOf(conn);
                        if (idx >= 0) {
                            userConns.set(idx, conn.updateActiveTime());
                        }
                        successCount++;
                    } else {
                        failedEntries.add(Map.entry(userId, conn));
                    }
                } catch (Exception e) {
                    failedEntries.add(Map.entry(userId, conn));
                }
            }
        }

        // 清理心跳失败的连接
        for (var failed : failedEntries) {
            doDisconnectOne(failed.getKey(), failed.getValue(), SseConstants.ConnectionStatus.ERROR);
        }

        if (!failedEntries.isEmpty()) {
            log.info("[SSE-Heartbeat] 心跳发送完成: success={}, failed={}", successCount, failedEntries.size());
        }
        return successCount;
    }

    // ==================== 连接清理 ====================

    /**
     * 清理超时连接（由定时任务调用）
     *
     * @return 被清理的连接数
     */
    @Override
    public int cleanupExpiredConnections() {
        List<Map.Entry<String, SseConnection>> expiredEntries = new ArrayList<>();

        for (var entry : connections.entrySet()) {
            String userId = entry.getKey();
            for (SseConnection conn : entry.getValue()) {
                if (conn.isExpired(timeout)) {
                    expiredEntries.add(Map.entry(userId, conn));
                }
            }
        }

        for (var expired : expiredEntries) {
            doDisconnectOne(expired.getKey(), expired.getValue(), SseConstants.ConnectionStatus.TIMEOUT);
        }

        if (!expiredEntries.isEmpty()) {
            log.info("[SSE-Cleanup] 清理超时连接: count={}, remainingUsers={}",
                    expiredEntries.size(), connections.size());
        }
        return expiredEntries.size();
    }

    // ==================== 运维指标 ====================

    /**
     * 获取运行时指标快照
     */
    public Map<String, Object> getMetrics() {
        int totalConns = getTotalConnectionCount();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("onlineUsers", connections.size());
        metrics.put("totalActiveConnections", totalConns);
        metrics.put("maxConnections", maxConnections);
        metrics.put("maxConnectionsPerUser", maxConnectionsPerUser);
        metrics.put("totalConnectionsCreated", totalConnections.get());
        metrics.put("totalMessagesSent", totalMessagesSent.get());
        metrics.put("totalMessagesFailed", totalMessagesFailed.get());
        return metrics;
    }

    // ==================== 私有方法 ====================

    /**
     * 断开单个连接
     */
    private void doDisconnectOne(String userId, SseConnection connection, String status) {
        int remainingConnections;
        synchronized (connections) {
            CopyOnWriteArrayList<SseConnection> userConns = connections.get(userId);
            if (userConns == null) return;
            boolean removed = userConns.removeIf(c -> c.connectionId().equals(connection.connectionId()));
            if (!removed) return;
            if (userConns.isEmpty()) {
                connections.remove(userId, userConns);
            }
            remainingConnections = userConns.size();
        }

        closeSink(connection);
        eventPublisher.publishEvent(new SseDisconnectEvent(this, connection.withStatus(status)));
        log.debug("SSE 单连接断开: userId={}, connId={}, status={}, remainingConns={}",
                userId, connection.connectionId(), status, remainingConnections);
    }

    private void closeSink(SseConnection connection) {
        if (connection.sink() != null) {
            try {
                connection.sink().tryEmitComplete();
            } catch (Exception e) {
                log.warn("关闭 SSE Sink 异常: userId={}, connId={}", connection.userId(), connection.connectionId(), e);
            }
        }
    }

    private void sendConnectMessage(String userId, String connectionId, Sinks.Many<String> sink) {
        try {
            SseMessage<String> connectMessage = SseMessage.connect(userId);
            String json = objectMapper.writeValueAsString(connectMessage);
            sink.tryEmitNext(json);
        } catch (Exception e) {
            log.error("发送连接成功消息失败: userId={}, connId={}", userId, connectionId, e);
        }
    }

    /**
     * 获取全局活跃连接总数
     */
    private int getTotalConnectionCount() {
        return connections.values().stream().mapToInt(List::size).sum();
    }
}
