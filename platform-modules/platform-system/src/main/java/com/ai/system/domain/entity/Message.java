package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 消息实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Entity
@Table(name = "sys_message", comment = "系统消息表")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Message extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "msg_title", nullable = false, length = 256, comment = "消息标题")
    private String msgTitle;

    @Column(name = "msg_content", nullable = false, length = 5000, comment = "消息内容")
    private String msgContent;

    @Column(name = "msg_type", nullable = false, length = 20, comment = "消息类型(1-系统公告,2-普通消息,3-工作提醒,4-审批通知)")
    private String msgType;

    @Column(name = "sender_id", length = 64, comment = "发送人ID")
    private String senderId;

    @Column(name = "sender_name", length = 128, comment = "发送人姓名")
    private String senderName;

    @Column(name = "receiver_id", nullable = false, length = 64, comment = "接收人ID")
    private String receiverId;

    @Column(name = "receiver_name", length = 128, comment = "接收人姓名")
    private String receiverName;

    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false", comment = "是否已读")
    private Boolean isRead = false;

    @Column(name = "read_time", comment = "读取时间")
    private LocalDateTime readTime;

    @Column(name = "display", nullable = false, columnDefinition = "boolean default false", comment = "是否展示在公告栏")
    private Boolean display = false;

    @Column(name = "priority", nullable = false, length = 20, columnDefinition = "varchar(20) default '2'", comment = "优先级(1-高,2-中,3-低)")
    private String priority = "2";

    @Column(name = "business_type", length = 64, comment = "关联业务类型")
    private String businessType;

    @Column(name = "business_id", length = 64, comment = "关联业务ID")
    private String businessId;
}
