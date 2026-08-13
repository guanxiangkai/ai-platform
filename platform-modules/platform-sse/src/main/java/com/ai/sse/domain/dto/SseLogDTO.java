package com.ai.sse.domain.dto;

import com.ai.sse.domain.entity.SseLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

@Schema(description = "SSE操作日志DTO")
@AutoMapper(target = SseLog.class)
public record SseLogDTO(
        @Schema(description = "执行状态") String status,
        @Schema(description = "消息") String message
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
