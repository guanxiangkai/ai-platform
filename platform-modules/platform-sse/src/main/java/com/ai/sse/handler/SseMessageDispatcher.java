package com.ai.sse.handler;

import com.ai.sse.model.SseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSE 消息处理器调度器
 * <p>
 * 策略模式 + 自动发现 — 根据消息类型（字典值）调度到对应处理器。
 * Spring 自动注入所有 {@link SseMessageHandler} Bean，
 * 根据 {@link SseMessageHandler#getSupportedType()} 自动分类注册。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Component
public class SseMessageDispatcher {

    private final Map<String, SseMessageHandler> handlers = new ConcurrentHashMap<>();
    private final List<SseMessageHandler> globalHandlers = new CopyOnWriteArrayList<>();

    /**
     * 运维指标 — 累计调度成功数
     */
    private final AtomicLong dispatchedCount = new AtomicLong();
    /**
     * 运维指标 — 累计调度失败数
     */
    private final AtomicLong failedCount = new AtomicLong();

    /**
     * 自动发现并注册所有 SseMessageHandler Bean
     */
    public SseMessageDispatcher(List<SseMessageHandler> allHandlers) {
        for (SseMessageHandler handler : allHandlers) {
            String type = handler.getSupportedType();
            if (type != null) {
                handlers.put(type, handler);
                log.info("[SSE-Dispatcher] 注册类型处理器: {} → {}", type, handler.getName());
            } else {
                globalHandlers.add(handler);
                log.info("[SSE-Dispatcher] 注册全局处理器: {}", handler.getName());
            }
        }
    }

    /**
     * 调度消息
     */
    public void dispatch(SseMessage<?> message) {
        if (message == null || message.type() == null) {
            log.warn("无效 SSE 消息，缺少消息类型");
            return;
        }

        String type = message.type();
        log.debug("调度消息: type={}", type);

        // 1. 执行类型特定处理器
        SseMessageHandler handler = handlers.get(type);
        if (handler != null) {
            try {
                handler.handle(message);
                dispatchedCount.incrementAndGet();
            } catch (Exception e) {
                failedCount.incrementAndGet();
                log.error("消息处理失败: type={}, handler={}, exception={}",
                        type, handler.getName(), e.getClass().getSimpleName());
            }
        }

        // 2. 执行全局处理器
        for (SseMessageHandler globalHandler : globalHandlers) {
            if (globalHandler.supports(message)) {
                try {
                    globalHandler.handle(message);
                    dispatchedCount.incrementAndGet();
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    log.error("全局处理器执行失败: handler={}, exception={}",
                            globalHandler.getName(), e.getClass().getSimpleName());
                }
            }
        }
    }

    public int getHandlerCount() {
        return handlers.size() + globalHandlers.size();
    }

    public long getDispatchedCount() {
        return dispatchedCount.get();
    }

    public long getFailedCount() {
        return failedCount.get();
    }
}
