package com.ai.sse.domain.vo;

import io.github.guanxiangkai.web.plus.core.annotation.DictField;
import com.ai.sse.domain.entity.SseConnectionRecord;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SSE 连接记录详情 VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@Schema(description = "SSE 连接记录详情VO")
@AutoMapper(target = SseConnectionRecord.class)
public class SseConnectionRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    private String id;

    @Schema(description = "连接唯一标识")
    private String connectionId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "连接状态")
    @DictField(type = "sse_connection_status")
    private String connectionStatus;

    @Schema(description = "连接状态名称")
    private String connectionStatusLabel;

    @Schema(description = "连接建立时间")
    private LocalDateTime connectTime;

    @Schema(description = "连接断开时间")
    private LocalDateTime disconnectTime;

    @Schema(description = "连接持续时长(秒)")
    private Long durationSeconds;

    @Schema(description = "断开原因")
    private String disconnectReason;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "服务实例标识")
    private String serverInstance;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String remark;
}
