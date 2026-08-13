package com.ai.agent.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 通用智能体会话中的有序消息。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_agent_message_record", comment = "智能体会话消息表")
public class AgentMessageRecord extends TenantEntity {
    @Column(name = "invocation_id", nullable = false, length = 128)
    private String invocationId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;
}
