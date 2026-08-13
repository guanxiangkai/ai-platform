package com.ai.sse.handler;

import com.ai.sse.constants.SseConstants;
import com.ai.sse.core.SseOperations;
import com.ai.sse.model.SseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳消息处理器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public final class HeartbeatHandler implements SseMessageHandler {

    private final SseOperations sseOperations;

    @Override
    public void handle(SseMessage<?> message) {
        String senderId = message.senderId();
        if (senderId == null) {
            return;
        }
        SseMessage<String> response = SseMessage.heartbeat(senderId);
        sseOperations.sendToUser(senderId, response);
        log.debug("心跳响应: userId={}", senderId);
    }

    @Override
    public String getSupportedType() {
        return SseConstants.MessageType.HEARTBEAT;
    }

    @Override
    public boolean supports(SseMessage<?> message) {
        return SseConstants.MessageType.HEARTBEAT.equals(message.type());
    }

    @Override
    public String getName() {
        return "HeartbeatHandler";
    }
}
