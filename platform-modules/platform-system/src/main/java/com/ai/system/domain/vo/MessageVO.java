package com.ai.system.domain.vo;

import io.github.guanxiangkai.web.plus.core.annotation.DictField;
import io.github.guanxiangkai.web.plus.core.domain.vo.BasePageVO;
import com.ai.system.constants.SystemConstants;
import com.ai.system.domain.entity.Message;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 消息VO
 * <p>
 * 继承 {@link BasePageVO}，复用 id
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息VO")
@AutoMapper(target = Message.class)
public class MessageVO extends BasePageVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息标题")
    private String msgTitle;

    @Schema(description = "消息内容")
    private String msgContent;

    @Schema(description = "消息类型(1-系统公告,2-普通消息,3-工作提醒,4-审批通知)")
    @DictField(type = SystemConstants.DictTypeConstants.SYS_MESSAGE_TYPE)
    private String msgType;

    @Schema(description = "消息类型名称")
    private String msgTypeLabel;

    @Schema(description = "发送人ID")
    private String senderId;

    @Schema(description = "发送人姓名")
    private String senderName;

    @Schema(description = "接收人ID")
    private String receiverId;

    @Schema(description = "接收人姓名")
    private String receiverName;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "读取时间")
    private LocalDateTime readTime;

    @Schema(description = "是否展示在公告栏")
    private Boolean display;

    @Schema(description = "优先级(1-高,2-中,3-低)")
    @DictField(type = SystemConstants.DictTypeConstants.SYS_MESSAGE_PRIORITY)
    private String priority;

    @Schema(description = "优先级名称")
    private String priorityLabel;

    @Schema(description = "关联业务类型")
    @DictField(type = SystemConstants.DictTypeConstants.SYS_BUSINESS_TYPE)
    private String businessType;

    @Schema(description = "关联业务类型名称")
    private String businessTypeLabel;

    @Schema(description = "关联业务ID")
    private String businessId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
