package com.ai.sse.domain.dto;

import com.ai.sse.domain.SseOperationType;
import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * SSE 操作日志分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SSE操作日志分页查询参数")
public class SseLogPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "操作类型（CONNECT/DISCONNECT/PUSH）")
    private SseOperationType operationType;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "目标类型（USER/TENANT/BROADCAST）")
    private String targetType;

    @Schema(description = "用户名（模糊）")
    private String username;

    @Schema(description = "执行状态（SUCCESS/FAIL）")
    private String status;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
