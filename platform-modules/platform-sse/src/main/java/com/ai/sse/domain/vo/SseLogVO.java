package com.ai.sse.domain.vo;

import com.ai.sse.domain.SseOperationType;
import com.ai.sse.domain.entity.SseLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SSE 操作日志详情 VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@Schema(description = "SSE操作日志详情VO")
@AutoMapper(target = SseLog.class)
public class SseLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;
    @Schema(description = "操作类型（CONNECT/DISCONNECT/PUSH）")
    private SseOperationType operationType;
    @Schema(description = "连接ID")
    private String connectionId;
    @Schema(description = "消息类型")
    private String messageType;
    @Schema(description = "目标类型")
    private String targetType;
    @Schema(description = "操作描述")
    private String description;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "推送内容")
    private String content;
    @Schema(description = "执行状态（SUCCESS/FAIL）")
    private String status;
    @Schema(description = "失败原因")
    private String failReason;
    @Schema(description = "执行耗时(ms)")
    private Long costMs;
    @Schema(description = "操作ID")
    private String operationId;
    @Schema(description = "TraceID")
    private String traceId;
    @Schema(description = "日志时间")
    private LocalDateTime logTime;
    @Schema(description = "租户ID")
    private String tenantId;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
