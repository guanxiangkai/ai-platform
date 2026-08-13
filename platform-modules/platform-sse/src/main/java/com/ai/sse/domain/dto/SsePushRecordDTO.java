package com.ai.sse.domain.dto;

import com.ai.sse.domain.entity.SsePushRecord;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE 推送日志 DTO（日志由系统自动写入，此 DTO 仅用于 BaseController/IBaseService 泛型约束）
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "SSE推送日志DTO")
@AutoMapper(target = SsePushRecord.class)
public record SsePushRecordDTO(
        @Schema(description = "推送状态") String pushStatus,
        @Schema(description = "失败原因") String failReason
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
