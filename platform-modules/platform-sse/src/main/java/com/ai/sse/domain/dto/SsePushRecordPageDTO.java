package com.ai.sse.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * SSE 推送日志分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SSE推送日志分页查询参数")
public class SsePushRecordPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "MQ消息ID")
    private String messageId;

    @Schema(description = "推送目标类型（USER/USERS/TENANT/BROADCAST）")
    private String targetType;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "目标用户ID")
    private String userId;

    @Schema(description = "推送状态（pending/success/skipped/failed）")
    private String pushStatus;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
