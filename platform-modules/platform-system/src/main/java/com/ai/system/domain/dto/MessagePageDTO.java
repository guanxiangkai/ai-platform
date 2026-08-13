package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 消息分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息分页查询参数")
public class MessagePageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息标题（模糊查询）")
    private String msgTitle;

    @Schema(description = "消息类型(1-系统公告,2-普通消息,3-工作提醒,4-审批通知)")
    private String msgType;

    @Schema(description = "发送人ID")
    private String senderId;

    @Schema(description = "接收人ID")
    private String receiverId;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "优先级(1-高,2-中,3-低)")
    private String priority;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
