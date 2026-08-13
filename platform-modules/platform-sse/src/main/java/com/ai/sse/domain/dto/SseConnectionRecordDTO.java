package com.ai.sse.domain.dto;

import com.ai.sse.domain.entity.SseConnectionRecord;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE 连接记录 DTO
 * <p>
 * 连接记录由系统事件监听器自动创建，不支持手动创建和编辑。
 * 此 DTO 仅用于满足 BaseController / IBaseService 的泛型约束。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "SSE 连接记录DTO")
@AutoMapper(target = SseConnectionRecord.class)
public record SseConnectionRecordDTO(

        @Schema(description = "连接状态(字典: sse_connection_status)")
        String connectionStatus,

        @Schema(description = "断开原因")
        String disconnectReason,

        @Schema(description = "备注")
        String remark

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
