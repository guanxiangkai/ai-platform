package com.ai.sse.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * SSE 推送日志实体
 * <p>
 * 对应数据库表 {@code sse_push_log}，记录每次 SSE 推送的完整结果。
 * 由 SSE 模块在推送完成后通过 MQ 消费 {@code log.sse-push} 主题写入。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sse_push_log", comment = "SSE推送日志表", indexes = {
        @Index(name = "idx_push_log_message_id", columnList = "message_id"),
        @Index(name = "idx_push_log_user_id", columnList = "user_id"),
        @Index(name = "idx_push_log_push_time", columnList = "push_time")
})
public class SsePushRecord extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * MQ 消息 ID
     */
    @Column(name = "message_id", length = 128, comment = "MQ消息ID")
    private String messageId;

    /**
     * 推送目标类型：USER / USERS / TENANT / BROADCAST
     */
    @Column(name = "target_type", length = 20, comment = "推送目标类型")
    private String targetType;

    /**
     * 消息类型（字典：sse_message_type）
     */
    @Column(name = "message_type", length = 50, comment = "消息类型")
    private String messageType;

    /**
     * 目标用户 ID（单用户推送）
     */
    @Column(name = "user_id", length = 64, comment = "目标用户ID")
    private String userId;

    /**
     * 目标用户 ID 列表（多用户推送，逗号分隔）
     */
    @Column(name = "user_ids", length = 2000, comment = "目标用户ID列表（逗号分隔）")
    private String userIds;

    /** 消息标题 */
    @Column(name = "push_title", length = 256, comment = "消息标题")
    private String pushTitle;

    /**
     * 推送内容（JSON）
     */
    @Column(name = "push_content", length = 5000, comment = "推送内容JSON")
    private String pushContent;

    /** 推送状态：pending / success / skipped / failed */
    @Column(name = "push_status", length = 20, comment = "推送状态")
    private String pushStatus;

    /** 失败/跳过原因 */
    @Column(name = "fail_reason", length = 500, comment = "失败原因")
    private String failReason;

    /**
     * 重试次数
     */
    @Column(name = "retry_count", comment = "重试次数")
    private Integer retryCount;

    /** 推送时间 */
    @Column(name = "push_time", comment = "推送时间")
    private LocalDateTime pushTime;
}
