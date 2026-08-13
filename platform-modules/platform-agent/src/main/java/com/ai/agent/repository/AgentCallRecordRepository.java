package com.ai.agent.repository;

import com.ai.agent.domain.entity.AgentCallRecord;
import com.ai.agent.domain.AgentInvocationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

/**
 * 智能体调用审计持久化接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface AgentCallRecordRepository extends JpaRepository<AgentCallRecord, String>,
        JpaSpecificationExecutor<AgentCallRecord> {

    Optional<AgentCallRecord> findByTenantIdAndInvocationCodeAndDeletedFalse(
            String tenantId, String invocationCode);

    /** 锁定调用状态以执行短事务收敛。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select call from AgentCallRecord call "
            + "where call.tenantId = :tenantId and call.invocationCode = :invocationId "
            + "and call.deleted = false")
    Optional<AgentCallRecord> findLockedByTenantIdAndInvocationId(
            @Param("tenantId") String tenantId, @Param("invocationId") String invocationId);

    /** 仅允许当前执行令牌把上游成功结果推进到可恢复状态。 */
    @Modifying(flushAutomatically = true)
    @Query("update AgentCallRecord call set "
            + "call.invocationState = :nextState, "
            + "call.providerResponseText = :responseText, call.providerConversationId = :conversationId, "
            + "call.inputTokens = :inputTokens, call.outputTokens = :outputTokens, "
            + "call.latencyMs = :latencyMs, call.responseSummary = :responseSummary, "
            + "call.version = call.version + 1 "
            + "where call.tenantId = :tenantId and call.invocationCode = :invocationId "
            + "and call.executionToken = :executionToken and call.invocationState = :expectedState "
            + "and call.deleted = false")
    int compareAndSetProviderSucceeded(
            @Param("tenantId") String tenantId,
            @Param("invocationId") String invocationId,
            @Param("executionToken") String executionToken,
            @Param("expectedState") AgentInvocationState expectedState,
            @Param("nextState") AgentInvocationState nextState,
            @Param("responseText") String responseText,
            @Param("conversationId") String conversationId,
            @Param("inputTokens") Integer inputTokens,
            @Param("outputTokens") Integer outputTokens,
            @Param("latencyMs") Long latencyMs,
            @Param("responseSummary") String responseSummary
    );

    /** 仅允许可恢复的上游成功状态进入最终成功状态。 */
    @Modifying(flushAutomatically = true)
    @Query("update AgentCallRecord call set "
            + "call.invocationState = :nextState, "
            + "call.assistantMessageId = :assistantMessageId, call.version = call.version + 1 "
            + "where call.tenantId = :tenantId and call.invocationCode = :invocationId "
            + "and call.invocationState = :expectedState and call.deleted = false")
    int compareAndSetSucceeded(
            @Param("tenantId") String tenantId,
            @Param("invocationId") String invocationId,
            @Param("expectedState") AgentInvocationState expectedState,
            @Param("nextState") AgentInvocationState nextState,
            @Param("assistantMessageId") String assistantMessageId
    );

    /** 仅允许当前执行令牌把调用收敛为失败。 */
    @Modifying(flushAutomatically = true)
    @Query("update AgentCallRecord call set "
            + "call.invocationState = :nextState, "
            + "call.latencyMs = :latencyMs, call.errorCode = :errorCode, call.errorMessage = :errorMessage, "
            + "call.version = call.version + 1 "
            + "where call.tenantId = :tenantId and call.invocationCode = :invocationId "
            + "and call.executionToken = :executionToken and call.invocationState = :expectedState "
            + "and call.deleted = false")
    int compareAndSetFailed(
            @Param("tenantId") String tenantId,
            @Param("invocationId") String invocationId,
            @Param("executionToken") String executionToken,
            @Param("expectedState") AgentInvocationState expectedState,
            @Param("nextState") AgentInvocationState nextState,
            @Param("latencyMs") Long latencyMs,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage
    );
}
