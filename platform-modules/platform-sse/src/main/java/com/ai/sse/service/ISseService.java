package com.ai.sse.service;

import com.ai.sse.model.SseMessage;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * SSE 服务门面接口
 * <p>
 * 对外提供连接管理、消息推送、在线查询等能力，
 * 内部委托 {@link com.ai.sse.core.SseOperations} 和
 * {@link com.ai.sse.handler.SseMessageDispatcher} 完成实际工作。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface ISseService {

    // ==================== 连接管理 ====================

    /**
     * 建立 SSE 连接
     */
    Flux<ServerSentEvent<String>> connect(String userId, String tenantId);

    /**
     * 断开 SSE 连接
     */
    void disconnect(String userId);

    /**
     * 用户是否在线
     */
    boolean isOnline(String userId);

    /**
     * 在线连接总数
     */
    int getOnlineCount();

    /**
     * 指定租户在线连接数
     */
    int getOnlineCountByTenant(String tenantId);

    // ==================== 消息推送 ====================

    /**
     * 推送消息给单个用户
     *
     * @param userId      目标用户
     * @param messageType 消息类型（字典值）
     * @param content     消息内容
     */
    void sendToUser(String userId, String messageType, Object content);

    /**
     * 推送消息给单个用户（完整消息体）
     */
    void sendToUser(String userId, SseMessage<?> message);

    /**
     * 推送消息给多个用户
     */
    void sendToUsers(List<String> userIds, Object content);

    /**
     * 全局广播
     */
    void broadcast(Object content);

    /**
     * 租户广播
     */
    void broadcastToTenant(String tenantId, Object content);

    // ==================== 消息调度 ====================

    /**
     * 调度消息到对应处理器
     */
    void dispatchMessage(SseMessage<?> message);
}
