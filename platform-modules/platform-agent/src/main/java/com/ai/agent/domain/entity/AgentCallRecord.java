package com.ai.agent.domain.entity;

import com.ai.agent.domain.AgentInvocationState;
import com.ai.agent.domain.AgentProviderType;
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
 * 不记录密钥的智能体调用状态与审计记录。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_agent_call_record", comment = "智能体调用记录表")
public class AgentCallRecord extends DataTenantEntity {
    /** 响应摘要持久化字符数上限。 */
    public static final int RESPONSE_SUMMARY_MAX_LENGTH = 4_000;

    /** 错误信息持久化字符数上限。 */
    public static final int ERROR_MESSAGE_MAX_LENGTH = 2_000;

    /** 请求指纹的 SHA-256 十六进制长度。 */
    public static final int REQUEST_FINGERPRINT_LENGTH = 64;

    @Column(name = "invocation_code", nullable = false, length = 128)
    private String invocationCode;

    @Column(name = "request_fingerprint", nullable = false, length = REQUEST_FINGERPRINT_LENGTH)
    private String requestFingerprint;

    @Column(name = "execution_token", nullable = false, length = 64)
    private String executionToken;

    @Column(name = "lease_expires_at", nullable = false)
    private LocalDateTime leaseExpiresAt;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 1;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_message_id", nullable = false, length = 64)
    private String userMessageId;

    @Column(name = "assistant_message_id", length = 64)
    private String assistantMessageId;

    @Column(name = "reserved_user_sequence_no", nullable = false)
    private Integer reservedUserSequenceNo;

    @Column(name = "reserved_assistant_sequence_no", nullable = false)
    private Integer reservedAssistantSequenceNo;

    @Column(name = "agent_id", nullable = false, length = 64)
    private String agentId;

    @Column(name = "agent_code", nullable = false, length = 128)
    private String agentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 32)
    private AgentProviderType providerType;

    @Column(name = "model_name", length = 256)
    private String modelName;

    @Column(name = "operation", nullable = false, length = 64)
    private String operation;

    @Enumerated(EnumType.STRING)
    @Column(name = "invocation_state", nullable = false, length = 32)
    private AgentInvocationState invocationState = AgentInvocationState.RUNNING;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "request_summary", length = 2000)
    private String requestSummary;

    @Column(name = "response_summary", length = RESPONSE_SUMMARY_MAX_LENGTH)
    private String responseSummary;

    @Column(name = "provider_response_text", columnDefinition = "text")
    private String providerResponseText;

    @Column(name = "provider_conversation_id", length = 256)
    private String providerConversationId;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String errorMessage;
}
