package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Message;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息DTO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "消息更新DTO")
@AutoMapper(target = Message.class)
public record MessageDTO(
        @Schema(description = "消息标题") String msgTitle,
        @Schema(description = "消息内容") String msgContent,
        @Schema(description = "消息类型(1-系统公告,2-普通消息,3-工作提醒,4-审批通知)") String msgType,
        @Schema(description = "发送人ID") String senderId,
        @Schema(description = "发送人姓名") String senderName,
        @Schema(description = "接收人ID") String receiverId,
        @Schema(description = "接收人姓名") String receiverName,
        @Schema(description = "优先级(1-高,2-中,3-低)") String priority,
        @Schema(description = "是否展示在公告栏") Boolean display,
        @Schema(description = "关联业务类型") String businessType,
        @Schema(description = "关联业务ID") String businessId
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
