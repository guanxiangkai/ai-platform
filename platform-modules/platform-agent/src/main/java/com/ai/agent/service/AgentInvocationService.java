package com.ai.agent.service;

import com.ai.agent.domain.AgentPublishState;
import com.ai.agent.domain.dto.AgentInvocationRequest;
import com.ai.agent.domain.entity.AgentConfig;
import com.ai.agent.domain.vo.AgentViews;
import com.ai.agent.integration.AgentProviderInvocation;
import com.ai.agent.integration.AgentProviderResult;
import com.ai.agent.integration.AgentProviderRegistry;
import com.ai.agent.repository.AgentConfigRepository;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 通用智能体调用编排。
 *
 * <p>会话序号和幂等记录在短事务内预留，提供方网络调用在事务外执行，
 * 上游结果先进入可恢复状态，再通过 CAS 短事务完成助手消息和会话。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AgentInvocationService {
    private final AgentManagementService management;
    private final AgentConfigRepository definitions;
    private final AgentProviderRegistry providers;
    private final AgentInvocationTransactionService transactions;

    /** 调用已发布且启用的智能体。 */
    public AgentViews.InvocationResult invoke(String agentId, AgentInvocationRequest request) {
        return invoke(agentId, request, false);
    }

    /** 管理端测试定义；允许尚未发布的草稿进入真实协议链路。 */
    public AgentViews.InvocationResult test(String agentId, AgentInvocationRequest request) {
        return invoke(agentId, request, true);
    }

    /** 供产品服务按稳定编码调用当前租户的已发布智能体。 */
    public AgentViews.InvocationResult invokeByCode(String agentCode, AgentInvocationRequest request) {
        if (!StringUtils.hasText(agentCode)) throw new BizException("智能体编码不能为空");
        String tenantId = currentTenantId();
        AgentConfig definition = definitions
                .findByTenantIdAndAgentCodeAndDeletedFalse(tenantId, agentCode.trim())
                .orElseThrow(() -> BizException.notFound("智能体定义"));
        return invoke(definition, request, false, tenantId);
    }

    private AgentViews.InvocationResult invoke(
            String agentId, AgentInvocationRequest request, boolean allowDraft) {
        return invoke(management.requireDefinition(agentId), request, allowDraft, currentTenantId());
    }

    private AgentViews.InvocationResult invoke(
            AgentConfig definition,
            AgentInvocationRequest request,
            boolean allowDraft,
            String tenantId) {
        validateRequest(request);
        validateAvailability(definition, allowDraft);
        String userId = currentUserId();
        String invocationId = StringUtils.hasText(request.invocationId())
                ? request.invocationId().trim()
                : UUID.randomUUID().toString();
        String fingerprint = AgentInvocationFingerprint.create(definition.getId(), request);
        AgentInvocationTransactionService.Reservation reservation = reserve(
                definition, request, tenantId, userId, invocationId, fingerprint);

        return switch (reservation.action()) {
            case RETURN_COMPLETED -> reservation.completedResult();
            case FINALIZE_SUCCESS -> transactions.finalizeSuccess(tenantId, invocationId, userId);
            case EXECUTE_PROVIDER -> executeProvider(definition, request, userId, reservation);
        };
    }

    private AgentInvocationTransactionService.Reservation reserve(
            AgentConfig definition,
            AgentInvocationRequest request,
            String tenantId,
            String userId,
            String invocationId,
            String fingerprint) {
        try {
            return transactions.reserve(definition, request, tenantId, userId, invocationId, fingerprint);
        } catch (DataIntegrityViolationException race) {
            // 不同会话对同一幂等标识的竞态由 PostgreSQL 唯一约束裁决，再读权威记录。
            return transactions.reserve(definition, request, tenantId, userId, invocationId, fingerprint);
        }
    }

    private AgentViews.InvocationResult executeProvider(
            AgentConfig definition,
            AgentInvocationRequest request,
            String userId,
            AgentInvocationTransactionService.Reservation reservation) {
        long startedAt = System.nanoTime();
        AgentProviderResult result;
        try {
            result = providers.require(definition.getProviderType())
                    .invoke(definition, new AgentProviderInvocation(
                            request,
                            userId,
                            reservation.invocationId(),
                            reservation.providerConversationId(),
                            reservation.history()
                    ));
        } catch (RuntimeException exception) {
            failReservation(reservation, exception, startedAt);
            if (exception instanceof BizException businessException) throw businessException;
            throw new BizException("智能体上游调用失败: " + safeMessage(exception));
        }

        transactions.recordProviderSuccess(reservation, result, elapsedMillis(startedAt));
        return transactions.finalizeSuccess(
                reservation.tenantId(), reservation.invocationId(), userId);
    }

    private void failReservation(
            AgentInvocationTransactionService.Reservation reservation,
            RuntimeException exception,
            long startedAt) {
        try {
            transactions.fail(reservation, exception, elapsedMillis(startedAt));
        } catch (RuntimeException persistenceFailure) {
            exception.addSuppressed(persistenceFailure);
        }
    }

    private void validateRequest(AgentInvocationRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new BizException("智能体调用消息不能为空");
        }
        if (StringUtils.hasText(request.invocationId()) && request.invocationId().trim().length() > 128) {
            throw new BizException("调用幂等标识长度不能超过 128");
        }
    }

    private void validateAvailability(AgentConfig definition, boolean allowDraft) {
        if (!Boolean.TRUE.equals(definition.getEnabled())) throw new BizException("智能体已停用");
        if (!allowDraft && definition.getPublishState() != AgentPublishState.PUBLISHED) {
            throw new BizException("智能体尚未发布");
        }
        if (!StringUtils.hasText(definition.getCredential())) throw new BizException("智能体尚未配置提供方密钥");
    }

    private String currentTenantId() {
        String tenantId = SecurityUtils.getTenantId();
        if (!StringUtils.hasText(tenantId) || "0".equals(tenantId)) throw new BizException("未获取到有效租户上下文");
        return tenantId.trim();
    }

    private String currentUserId() {
        String userId = SecurityUtils.getUserId();
        if (!StringUtils.hasText(userId)) throw new BizException("未获取到当前用户");
        return userId.trim();
    }

    private String safeMessage(Throwable value) {
        return StringUtils.hasText(value.getMessage()) ? value.getMessage() : value.getClass().getSimpleName();
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }
}
