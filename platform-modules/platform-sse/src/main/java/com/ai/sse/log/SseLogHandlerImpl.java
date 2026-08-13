package com.ai.sse.log;

import com.ai.sse.domain.SseOperationType;
import io.github.guanxiangkai.web.plus.log.entity.BaseLog;
import io.github.guanxiangkai.web.plus.log.spi.SseLogHandler;
import com.ai.sse.domain.entity.SseLog;
import com.ai.sse.service.ISseLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SSE 操作日志 SPI 实现。
 *
 * <p>{@code SseLogAspect} 采集 {@code @SseLog} 标注方法的执行信息，
 * 本处理器保证日志与当前调用同步完成持久化。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseLogHandlerImpl implements SseLogHandler {

    private final ISseLogService sseLogService;

    @Override
    public void handle(BaseLog baseLog) {
        if (!(baseLog instanceof SseLog entity)) {
            throw new IllegalArgumentException("SSE 日志必须使用 SseLog 当前契约");
        }
        entity.setOperationType(SseOperationType.PUSH);
        sseLogService.createEntity(entity);
        log.debug("[SseLogHandler] SSE操作日志保存成功: type={}, target={}, user={}, cost={}ms",
                entity.getMessageType(), entity.getTargetType(), entity.getUsername(), entity.getCostMs());
    }
}
