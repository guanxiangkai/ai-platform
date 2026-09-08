package com.ai.sse.domain.vo;

import io.github.guanxiangkai.web.plus.core.annotation.DictField;
import com.ai.sse.domain.entity.SsePushRecord;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推送记录详情 VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@Schema(description = "推送记录详情VO")
@AutoMapper(target = SsePushRecord.class)
public class SsePushRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    private String id;

    @Schema(description = "MQ 消息ID")
    private String messageId;

    @Schema(description = "推送目标类型")
    @DictField(type = "sse_target_type")
    private String targetType;

    @Schema(description = "推送目标类型名称")
    private String targetTypeLabel;

    @Schema(description = "消息类型")
    @DictField(type = "sse_message_type")
    private String messageType;

    @Schema(description = "消息类型名称")
    private String messageTypeLabel;

    @Schema(description = "目标用户ID")
    private String userId;

    @Schema(description = "目标用户ID列表（逗号分隔）")
    private String userIds;

    @Schema(description = "消息标题")
    private String pushTitle;

    @Schema(description = "消息内容(JSON)")
    private String pushContent;

    @Schema(description = "推送状态")
    @DictField(type = "sse_push_status")
    private String pushStatus;

    @Schema(description = "推送状态名称")
    private String pushStatusLabel;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "推送时间")
    private LocalDateTime pushTime;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String remark;
}
