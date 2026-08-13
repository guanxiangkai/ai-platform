package com.ai.sse.scheduler;

import com.ai.sse.config.SseProperties;
import com.ai.sse.core.DefaultSseOperations;
import com.ai.sse.core.SseOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * SSE 服务端心跳调度器
 * <p>
 * 定时向所有在线 SSE 连接发送心跳消息，实现：
 * <ul>
 *   <li>防止连接被反向代理（Nginx / ALB）或防火墙因空闲超时断开</li>
 *   <li>更新连接的 {@code lastActiveTime}，为超时清理提供准确依据</li>
 *   <li>检测已失效连接（Sink 写入失败），主动回收资源</li>
 * </ul>
 * </p>
 * <p>
 * 心跳间隔由 {@link SseProperties#heartbeatInterval()} 控制（默认 30 秒）。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
public class SseHeartbeatScheduler {

    private final SseOperations sseOperations;

    public SseHeartbeatScheduler(SseOperations sseOperations) {
        this.sseOperations = sseOperations;
    }

    /**
     * 定时发送心跳（默认每 30 秒执行一次）
     */
    @Scheduled(fixedDelayString = "${ai.sse.heartbeat-interval:30000}")
    public void sendHeartbeat() {
        if (!(sseOperations instanceof DefaultSseOperations ops)) {
            return;
        }
        int onlineCount = ops.getOnlineCount();
        if (onlineCount == 0) {
            return;
        }
        int success = ops.sendHeartbeatToAll();
        log.debug("[SSE-Heartbeat] 心跳完成: success={}, onlineUsers={}", success, onlineCount);
    }
}
