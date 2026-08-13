package com.ai.agent.domain.entity;

import com.ai.agent.domain.AgentVoiceRecognitionState;
import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 通用语音转写请求及结果审计。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_agent_voice_record", comment = "智能体语音文件记录表")
public class AgentVoiceRecord extends DataTenantEntity {
    /** 文件名持久化字符数上限。 */
    public static final int FILE_NAME_MAX_LENGTH = 256;

    /** 媒体类型持久化字符数上限。 */
    public static final int CONTENT_TYPE_MAX_LENGTH = 128;

    /** 语言标识持久化字符数上限。 */
    public static final int LANGUAGE_MAX_LENGTH = 32;

    /** 转写错误信息持久化字符数上限。 */
    public static final int ERROR_MESSAGE_MAX_LENGTH = 2_000;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "file_name", nullable = false, length = FILE_NAME_MAX_LENGTH)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = CONTENT_TYPE_MAX_LENGTH)
    private String contentType;

    @Column(name = "content_length", nullable = false)
    private Long contentLength;

    @Column(name = "language", length = LANGUAGE_MAX_LENGTH)
    private String language;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "transcript", columnDefinition = "text")
    private String transcript;

    @Enumerated(EnumType.STRING)
    @Column(name = "recognition_status", nullable = false, length = 32)
    private AgentVoiceRecognitionState recognitionStatus;

    @Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String errorMessage;
}
