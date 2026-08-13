package com.ai.agent.domain.entity;

import com.ai.agent.domain.AgentSessionState;
import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 租户内、与具体产品无关的智能体会话。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_agent_session_record", comment = "智能体会话记录表")
public class AgentSessionRecord extends DataTenantEntity {
    /** 会话标题持久化字符数上限。 */
    public static final int TITLE_MAX_LENGTH = 256;

    /** 提供方会话标识持久化字符数上限。 */
    public static final int PROVIDER_CONVERSATION_ID_MAX_LENGTH = 256;

    @Column(name = "session_code", nullable = false, length = 128)
    private String sessionCode;

    @Column(name = "agent_id", nullable = false, length = 64)
    private String agentId;

    @Column(name = "agent_code", nullable = false, length = 128)
    private String agentCode;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "title", length = TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "context_namespace", length = 128)
    private String contextNamespace;

    @Column(name = "context_reference", length = 256)
    private String contextReference;

    @Column(name = "context_json", columnDefinition = "text")
    private String contextJson;

    @Column(name = "provider_conversation_id", length = PROVIDER_CONVERSATION_ID_MAX_LENGTH)
    private String providerConversationId;

    /** 当前占用会话顺序边界的调用幂等标识。 */
    @Column(name = "active_invocation_id", length = 128)
    private String activeInvocationId;

    /** 已持久化或已被活动调用预留的最大消息序号。 */
    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_state", nullable = false, length = 32)
    private AgentSessionState sessionState = AgentSessionState.ACTIVE;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
