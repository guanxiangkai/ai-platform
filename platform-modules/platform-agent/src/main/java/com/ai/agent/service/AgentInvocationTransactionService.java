package com.ai.agent.service;

import com.ai.agent.config.AgentInvocationProperties;
import com.ai.agent.domain.AgentInvocationState;
import com.ai.agent.domain.AgentMessageRole;
import com.ai.agent.domain.AgentSessionState;
import com.ai.agent.domain.dto.AgentInvocationRequest;
import com.ai.agent.domain.entity.AgentCallRecord;
import com.ai.agent.domain.entity.AgentConfig;
import com.ai.agent.domain.entity.AgentMessageRecord;
import com.ai.agent.domain.entity.AgentSessionRecord;
import com.ai.agent.domain.vo.AgentViews;
import com.ai.agent.integration.AgentProviderMessage;
import com.ai.agent.integration.AgentProviderResult;
import com.ai.agent.repository.AgentCallRecordRepository;
import com.ai.agent.repository.AgentMessageRecordRepository;
import com.ai.agent.repository.AgentSessionRecordRepository;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 智能体调用的短事务状态机。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
class AgentInvocationTransactionService {
    private static final int GENERATED_TITLE_MAX_LENGTH = 80;

    private final AgentSessionRecordRepository sessions;
    private final AgentMessageRecordRepository messages;
    private final AgentCallRecordRepository calls;
    private final AgentInvocationProperties properties;

    /** 锁定会话并预留用户、助手消息序号，事务内不调用上游。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation reserve(
            AgentConfig definition,
            AgentInvocationRequest request,
            String tenantId,
            String userId,
            String invocationId,
            String requestFingerprint) {
        AgentCallRecord existing = calls.findLockedByTenantIdAndInvocationId(tenantId, invocationId).orElse(null);
        if (existing != null) {
            return existingReservation(existing, definition, request, tenantId, userId, requestFingerprint);
        }

        AgentSessionRecord session = resolveLockedSession(definition, request, tenantId, userId);
        existing = calls.findLockedByTenantIdAndInvocationId(tenantId, invocationId).orElse(null);
        if (existing != null) {
            return existingReservation(existing, definition, request, tenantId, userId, requestFingerprint);
        }
        if (StringUtils.hasText(session.getActiveInvocationId())) {
            throw new BizException("当前会话存在进行中的调用");
        }

        List<AgentProviderMessage> history = boundedHistory(session, null);
        int userSequence = session.getMessageCount() + 1;
        int assistantSequence = userSequence + 1;
        AgentMessageRecord userMessage = message(
                session, invocationId, userSequence, AgentMessageRole.USER, request.message());
        messages.saveAndFlush(userMessage);

        String executionToken = UUID.randomUUID().toString();
        AgentCallRecord call = new AgentCallRecord();
        call.setTenantId(tenantId);
        call.setInvocationCode(invocationId);
        call.setRequestFingerprint(requestFingerprint);
        call.setExecutionToken(executionToken);
        call.setLeaseExpiresAt(LocalDateTime.now().plus(properties.getExecutionLease()));
        call.setAttemptCount(1);
        call.setSessionId(session.getId());
        call.setUserMessageId(userMessage.getId());
        call.setReservedUserSequenceNo(userSequence);
        call.setReservedAssistantSequenceNo(assistantSequence);
        call.setAgentId(definition.getId());
        call.setAgentCode(definition.getAgentCode());
        call.setProviderType(definition.getProviderType());
        call.setModelName(definition.getModelName());
        call.setOperation(definition.getInvocationMode().name());
        call.setInvocationState(AgentInvocationState.RUNNING);
        int variableCount = request.variables() == null ? 0 : request.variables().size();
        call.setRequestSummary("messageLength=" + request.message().length()
                + ", variableCount=" + variableCount);
        calls.saveAndFlush(call);

        session.setActiveInvocationId(invocationId);
        session.setMessageCount(assistantSequence);
        sessions.save(session);
        return Reservation.execute(invocationId, executionToken, session, history);
    }

    /** 把上游成功响应保存为可重入完成的中间状态。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordProviderSuccess(
            Reservation reservation, AgentProviderResult result, long latencyMs) {
        int changed = calls.compareAndSetProviderSucceeded(
                reservation.tenantId(),
                reservation.invocationId(),
                reservation.executionToken(),
                AgentInvocationState.RUNNING,
                AgentInvocationState.PROVIDER_SUCCEEDED,
                result.text(),
                limit(result.providerConversationId(), AgentSessionRecord.PROVIDER_CONVERSATION_ID_MAX_LENGTH),
                nonNegative(result.inputTokens()),
                nonNegative(result.outputTokens()),
                latencyMs,
                limit(result.text(), AgentCallRecord.RESPONSE_SUMMARY_MAX_LENGTH)
        );
        if (changed != 1) {
            throw new BizException("调用执行权已变更，不能写入过期的上游结果");
        }
    }

    /** 从可恢复的上游成功状态幂等完成助手消息和会话状态。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentViews.InvocationResult finalizeSuccess(String tenantId, String invocationId, String userId) {
        AgentCallRecord call = calls.findLockedByTenantIdAndInvocationId(tenantId, invocationId)
                .orElseThrow(() -> BizException.notFound("智能体调用"));
        AgentSessionRecord session = lockedOwnedSession(call.getSessionId(), tenantId, userId);
        if (call.getInvocationState() == AgentInvocationState.SUCCEEDED) {
            return completed(call, session);
        }
        verifyActiveInvocation(session, invocationId);
        if (call.getInvocationState() != AgentInvocationState.PROVIDER_SUCCEEDED) {
            throw new BizException("智能体调用尚未获得可完成的上游结果");
        }

        AgentMessageRecord assistant = message(
                session,
                invocationId,
                call.getReservedAssistantSequenceNo(),
                AgentMessageRole.ASSISTANT,
                call.getProviderResponseText()
        );
        messages.saveAndFlush(assistant);
        int changed = calls.compareAndSetSucceeded(
                tenantId,
                invocationId,
                AgentInvocationState.PROVIDER_SUCCEEDED,
                AgentInvocationState.SUCCEEDED,
                assistant.getId()
        );
        if (changed != 1) {
            throw new BizException("智能体调用完成状态已变更");
        }

        session.setActiveInvocationId(null);
        session.setMessageCount(call.getReservedAssistantSequenceNo());
        session.setSessionState(AgentSessionState.ACTIVE);
        if (StringUtils.hasText(call.getProviderConversationId())) {
            session.setProviderConversationId(call.getProviderConversationId());
        }
        sessions.save(session);
        return result(call, session, assistant);
    }

    /** 仅允许当前执行令牌把调用收敛为失败并释放会话。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(
            Reservation reservation, RuntimeException exception, long latencyMs) {
        AgentCallRecord call = calls.findLockedByTenantIdAndInvocationId(
                        reservation.tenantId(), reservation.invocationId())
                .orElseThrow(() -> BizException.notFound("智能体调用"));
        AgentSessionRecord session = sessions.findLockedByIdAndTenantId(
                        call.getSessionId(), reservation.tenantId())
                .orElseThrow(() -> BizException.notFound("智能体会话"));
        int changed = calls.compareAndSetFailed(
                reservation.tenantId(),
                reservation.invocationId(),
                reservation.executionToken(),
                AgentInvocationState.RUNNING,
                AgentInvocationState.FAILED,
                latencyMs,
                exception.getClass().getSimpleName(),
                publicErrorMessage(exception)
        );
        if (changed != 1) {
            return;
        }
        if (Objects.equals(session.getActiveInvocationId(), reservation.invocationId())) {
            session.setActiveInvocationId(null);
            session.setMessageCount(call.getReservedUserSequenceNo());
            session.setSessionState(AgentSessionState.ACTIVE);
            sessions.save(session);
        }
    }

    private Reservation existingReservation(
            AgentCallRecord call,
            AgentConfig definition,
            AgentInvocationRequest request,
            String tenantId,
            String userId,
            String requestFingerprint) {
        if (!Objects.equals(call.getAgentId(), definition.getId())
                || !Objects.equals(call.getRequestFingerprint(), requestFingerprint)) {
            throw new BizException("调用幂等标识已用于不同请求");
        }
        AgentSessionRecord session = lockedOwnedSession(call.getSessionId(), tenantId, userId);
        if (call.getInvocationState() == AgentInvocationState.SUCCEEDED) {
            return Reservation.completed(completed(call, session));
        }
        if (call.getInvocationState() == AgentInvocationState.PROVIDER_SUCCEEDED) {
            verifyActiveInvocation(session, call.getInvocationCode());
            return Reservation.finalizePending(tenantId, call.getInvocationCode());
        }
        if (call.getInvocationState() == AgentInvocationState.FAILED) {
            throw new BizException("相同调用幂等标识已失败: " + call.getErrorMessage());
        }
        verifyActiveInvocation(session, call.getInvocationCode());
        if (call.getLeaseExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BizException("相同调用正在处理中");
        }

        String executionToken = UUID.randomUUID().toString();
        call.setExecutionToken(executionToken);
        call.setLeaseExpiresAt(LocalDateTime.now().plus(properties.getExecutionLease()));
        call.setAttemptCount(call.getAttemptCount() + 1);
        calls.saveAndFlush(call);
        return Reservation.execute(
                call.getInvocationCode(), executionToken, session,
                boundedHistory(session, call.getInvocationCode()));
    }

    private AgentSessionRecord resolveLockedSession(
            AgentConfig definition, AgentInvocationRequest request, String tenantId, String userId) {
        if (StringUtils.hasText(request.sessionId())) {
            AgentSessionRecord session = lockedOwnedSession(request.sessionId(), tenantId, userId);
            if (!Objects.equals(session.getAgentId(), definition.getId())) {
                throw new BizException("会话不属于当前智能体");
            }
            if (session.getSessionState() != AgentSessionState.ACTIVE) {
                throw new BizException("智能体会话已结束");
            }
            return session;
        }
        AgentSessionRecord session = new AgentSessionRecord();
        session.setTenantId(tenantId);
        session.setSessionCode(UUID.randomUUID().toString());
        session.setAgentId(definition.getId());
        session.setAgentCode(definition.getAgentCode());
        session.setUserId(userId);
        session.setTitle(limit(trim(request.sessionTitle(), title(request.message())),
                AgentSessionRecord.TITLE_MAX_LENGTH));
        session.setContextNamespace(trim(request.contextNamespace(), null));
        session.setContextReference(trim(request.contextReference(), null));
        session.setMessageCount(0);
        session.setSessionState(AgentSessionState.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        return sessions.saveAndFlush(session);
    }

    private AgentSessionRecord lockedOwnedSession(String sessionId, String tenantId, String userId) {
        AgentSessionRecord session = sessions.findLockedByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> BizException.notFound("智能体会话"));
        if (!Objects.equals(userId, session.getUserId())) {
            throw new BizException("无权访问其他用户的智能体会话");
        }
        return session;
    }

    private void verifyActiveInvocation(AgentSessionRecord session, String invocationId) {
        if (!Objects.equals(session.getActiveInvocationId(), invocationId)) {
            throw new BizException("会话活动调用与调用记录不一致");
        }
    }

    private List<AgentProviderMessage> boundedHistory(
            AgentSessionRecord session, String excludedInvocationId) {
        List<AgentMessageRecord> stored = messages
                .findBySessionIdAndTenantIdOrderBySequenceNoAsc(session.getId(), session.getTenantId());
        List<AgentProviderMessage> selected = new ArrayList<>();
        int characters = 0;
        for (int index = stored.size() - 1;
             index >= 0 && selected.size() < properties.getMaxHistoryMessages();
             index--) {
            AgentMessageRecord message = stored.get(index);
            if (Objects.equals(excludedInvocationId, message.getInvocationId())
                    || !AgentMessageRole.contains(message.getRole())
                    || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            String content = message.getContent();
            if (!selected.isEmpty() && characters + content.length() > properties.getMaxHistoryCharacters()) {
                break;
            }
            if (selected.isEmpty() && content.length() > properties.getMaxHistoryCharacters()) {
                content = content.substring(content.length() - properties.getMaxHistoryCharacters());
            }
            selected.add(new AgentProviderMessage(message.getRole(), content));
            characters += content.length();
        }
        return selected.reversed();
    }

    private AgentMessageRecord message(
            AgentSessionRecord session,
            String invocationId,
            int sequence,
            AgentMessageRole role,
            String content) {
        AgentMessageRecord message = new AgentMessageRecord();
        message.setTenantId(session.getTenantId());
        message.setInvocationId(invocationId);
        message.setSessionId(session.getId());
        message.setSequenceNo(sequence);
        message.setRole(role.value());
        message.setContent(content);
        return message;
    }

    private AgentViews.InvocationResult completed(AgentCallRecord call, AgentSessionRecord session) {
        if (!StringUtils.hasText(call.getAssistantMessageId())) {
            throw new BizException("智能体调用缺少已完成的助手消息");
        }
        AgentMessageRecord assistant = messages.findByIdAndTenantId(
                        call.getAssistantMessageId(), session.getTenantId())
                .orElseThrow(() -> BizException.notFound("智能体助手消息"));
        return result(call, session, assistant);
    }

    private AgentViews.InvocationResult result(
            AgentCallRecord call, AgentSessionRecord session, AgentMessageRecord assistant) {
        return new AgentViews.InvocationResult(
                session.getId(),
                call.getInvocationCode(),
                assistant.getId(),
                assistant.getContent(),
                call.getInputTokens(),
                call.getOutputTokens()
        );
    }

    private String title(String value) {
        return limit(value.replaceAll("\\s+", " ").trim(), GENERATED_TITLE_MAX_LENGTH);
    }

    private String trim(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String publicErrorMessage(Throwable value) {
        if (value instanceof BizException && StringUtils.hasText(value.getMessage())) {
            return limit(value.getMessage(), AgentCallRecord.ERROR_MESSAGE_MAX_LENGTH);
        }
        return "智能体上游调用失败";
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private Integer nonNegative(Integer value) {
        return value == null ? null : Math.max(0, value);
    }

    enum ReservationAction {
        EXECUTE_PROVIDER,
        FINALIZE_SUCCESS,
        RETURN_COMPLETED
    }

    record Reservation(
            ReservationAction action,
            String tenantId,
            String invocationId,
            String executionToken,
            String sessionId,
            String providerConversationId,
            List<AgentProviderMessage> history,
            AgentViews.InvocationResult completedResult) {

        Reservation {
            history = history == null ? List.of() : List.copyOf(history);
        }

        @Override
        public String toString() {
            return "Reservation[action=" + action
                    + ", identity=<redacted>, executionToken=<redacted>, historySize=" + history.size()
                    + ", completed=" + (completedResult != null) + ']';
        }

        static Reservation execute(
                String invocationId,
                String executionToken,
                AgentSessionRecord session,
                List<AgentProviderMessage> history) {
            return new Reservation(
                    ReservationAction.EXECUTE_PROVIDER,
                    session.getTenantId(),
                    invocationId,
                    executionToken,
                    session.getId(),
                    session.getProviderConversationId(),
                    history,
                    null
            );
        }

        static Reservation finalizePending(String tenantId, String invocationId) {
            return new Reservation(
                    ReservationAction.FINALIZE_SUCCESS,
                    tenantId,
                    invocationId,
                    null,
                    null,
                    null,
                    List.of(),
                    null
            );
        }

        static Reservation completed(AgentViews.InvocationResult result) {
            return new Reservation(
                    ReservationAction.RETURN_COMPLETED,
                    null,
                    result.invocationId(),
                    null,
                    result.sessionId(),
                    null,
                    List.of(),
                    result
            );
        }
    }
}
