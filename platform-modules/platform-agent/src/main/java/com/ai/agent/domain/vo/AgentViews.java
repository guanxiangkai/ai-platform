package com.ai.agent.domain.vo;

import com.ai.agent.domain.AgentInvocationMode;
import com.ai.agent.domain.AgentInvocationState;
import com.ai.agent.domain.AgentProviderType;
import com.ai.agent.domain.AgentPublishState;
import com.ai.agent.domain.AgentSessionState;
import com.ai.agent.domain.entity.AgentCallRecord;
import com.ai.agent.domain.entity.AgentConfig;
import com.ai.agent.domain.entity.AgentMessageRecord;
import com.ai.agent.domain.entity.AgentSessionRecord;
import com.ai.agent.domain.entity.AgentVoiceRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通用智能体管理与运行 API 的稳定视图。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class AgentViews {
    private AgentViews() {
    }

    /**
     * 智能体定义，不暴露加密凭据。
     *
     * @author guanxiangkai
     * @since 1.0.0
     */
    public record Definition(
            String id,
            String agentCode,
            String agentName,
            String description,
            AgentProviderType providerType,
            AgentInvocationMode invocationMode,
            String endpointUrl,
            String modelName,
            boolean credentialConfigured,
            String systemPrompt,
            Double temperature,
            String runtimeConfig,
            AgentPublishState publishState,
            Integer revision,
            Boolean enabled,
            String remark,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
        /** 从持久化实体生成脱敏视图。 */
        public static Definition from(AgentConfig value) {
            return new Definition(value.getId(), value.getAgentCode(), value.getAgentName(),
                    value.getDescription(), value.getProviderType(), value.getInvocationMode(),
                    value.getEndpointUrl(), value.getModelName(),
                    value.getCredential() != null && !value.getCredential().isBlank(),
                    value.getSystemPrompt(), value.getTemperature(), value.getRuntimeConfig(),
                    value.getPublishState(), value.getRevision(), value.getEnabled(), value.getRemark(),
                    value.getCreateTime(), value.getUpdateTime());
        }
    }

    /**
     * 会话及其可选消息明细。
     *
     * @author guanxiangkai
     * @since 1.0.0
     */
    public record Session(
            String id,
            String sessionCode,
            String agentId,
            String agentCode,
            String userId,
            String title,
            String contextNamespace,
            String contextReference,
            String contextJson,
            Integer messageCount,
            AgentSessionState sessionState,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            LocalDateTime createTime,
            LocalDateTime updateTime,
            List<Message> messages
    ) {
        /** 从会话实体生成视图。 */
        public static Session from(AgentSessionRecord value, List<Message> messages) {
            return new Session(value.getId(), value.getSessionCode(), value.getAgentId(),
                    value.getAgentCode(), value.getUserId(), value.getTitle(),
                    value.getContextNamespace(), value.getContextReference(), value.getContextJson(),
                    value.getMessageCount(), value.getSessionState(), value.getStartedAt(), value.getEndedAt(),
                    value.getCreateTime(), value.getUpdateTime(), messages);
        }
    }

    /**
     * 会话消息。
     *
     * @author guanxiangkai
     * @since 1.0.0
     */
    public record Message(
            String id,
            Integer sequenceNo,
            String role,
            String content,
            String metadataJson,
            LocalDateTime createTime
    ) {
        /** 从消息实体生成视图。 */
        public static Message from(AgentMessageRecord value) {
            return new Message(value.getId(), value.getSequenceNo(), value.getRole(), value.getContent(),
                    value.getMetadataJson(), value.getCreateTime());
        }
    }

    /**
     * 单次调用审计。
     *
     * @author guanxiangkai
     * @since 1.0.0
     */
    public record Invocation(
            String id,
            String invocationCode,
            String sessionId,
            String agentId,
            String agentCode,
            AgentProviderType providerType,
            String modelName,
            String operation,
            AgentInvocationState invocationState,
            Integer inputTokens,
            Integer outputTokens,
            Long latencyMs,
            String requestSummary,
            String responseSummary,
            String errorCode,
            String errorMessage,
            LocalDateTime createTime
    ) {
        /** 从调用审计实体生成视图。 */
        public static Invocation from(AgentCallRecord value) {
            return new Invocation(value.getId(), value.getInvocationCode(), value.getSessionId(),
                    value.getAgentId(), value.getAgentCode(), value.getProviderType(), value.getModelName(),
                    value.getOperation(), value.getInvocationState(), value.getInputTokens(),
                    value.getOutputTokens(), value.getLatencyMs(), value.getRequestSummary(),
                    value.getResponseSummary(), value.getErrorCode(), value.getErrorMessage(), value.getCreateTime());
        }
    }

    /**
     * 语音转写审计。
     *
     * @author guanxiangkai
     * @since 1.0.0
     */
    public record Voice(
            String id,
            String sessionId,
            String userId,
            String fileName,
            String contentType,
            Long contentLength,
            String language,
            Integer durationSeconds,
            String transcript,
            String recognitionStatus,
            String errorMessage,
            LocalDateTime createTime
    ) {
        /** 从语音审计实体生成视图。 */
        public static Voice from(AgentVoiceRecord value) {
            return new Voice(value.getId(), value.getSessionId(), value.getUserId(), value.getFileName(),
                    value.getContentType(), value.getContentLength(), value.getLanguage(),
                    value.getDurationSeconds(), value.getTranscript(), value.getRecognitionStatus().name(),
                    value.getErrorMessage(), value.getCreateTime());
        }
    }

    /**
     * 智能体调用结果。
     *
     * @author guanxiangkai
     * @since 1.0.0
     */
    public record InvocationResult(
            String sessionId,
            String invocationId,
            String messageId,
            String text,
            Integer inputTokens,
            Integer outputTokens
    ) {
    }
}
