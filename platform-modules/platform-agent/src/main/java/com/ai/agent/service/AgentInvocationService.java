package com.ai.agent.service;

import com.ai.agent.domain.AgentPublishState;
import com.ai.agent.domain.dto.AgentInvocationRequest;
import com.ai.agent.domain.entity.AgentConfig;
import com.ai.agent.domain.vo.AgentViews;
import com.ai.agent.integration.AgentProviderInvocation;
import com.ai.agent.integration.AgentProviderResult;
import com.ai.agent.integration.AgentProviderStreamEvent;
import com.ai.agent.integration.AgentProviderRegistry;
import com.ai.agent.repository.AgentConfigRepository;
import io.github.guanxiangkai.jpa.plus.core.field.FieldEngine;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final String PROVIDER_FAILURE_MESSAGE = "智能体服务暂不可用，请稍后重试";
    private final AgentManagementService management;
    private final AgentConfigRepository definitions;
    private final FieldEngine fieldEngine;
    private final AgentProviderRegistry providers;
    private final AgentInvocationTransactionService transactions;

    /** 调用已发布且启用的智能体。 */
    public AgentViews.InvocationResult invoke(String agentId, AgentInvocationRequest request) {
        return invoke(agentId, request, request.message(), false);
    }

    /** 调用已发布且启用的智能体并返回统一流式事件。 */
    public Flux<StreamEvent> stream(String agentId, AgentInvocationRequest request) {
        return stream(agentId, request, request.message());
    }

    AgentViews.InvocationResult invoke(
            String agentId, AgentInvocationRequest request, String providerMessage) {
        return invoke(agentId, request, providerMessage, false);
    }

    Flux<StreamEvent> stream(
            String agentId, AgentInvocationRequest request, String providerMessage) {
        return stream(
                management.requireDefinition(agentId), request, providerMessage, false, currentTenantId());
    }

    /** 管理端测试定义；允许尚未发布的草稿进入真实协议链路。 */
    public AgentViews.InvocationResult test(String agentId, AgentInvocationRequest request) {
        return invoke(agentId, request, request.message(), true);
    }

    /** 供产品服务按稳定编码调用当前租户的已发布智能体。 */
    public AgentViews.InvocationResult invokeByCode(String agentCode, AgentInvocationRequest request) {
        if (!StringUtils.hasText(agentCode)) throw new BizException("智能体编码不能为空");
        String tenantId = currentTenantId();
        AgentConfig definition = definitions
                .findByTenantIdAndAgentCodeAndDeletedFalse(tenantId, agentCode.trim())
                .orElseThrow(() -> BizException.notFound("智能体定义"));
        return invoke(definition, request, request.message(), false, tenantId);
    }

    private AgentViews.InvocationResult invoke(
            String agentId, AgentInvocationRequest request, String providerMessage, boolean allowDraft) {
        return invoke(
                management.requireDefinition(agentId), request, providerMessage, allowDraft, currentTenantId());
    }

    private Flux<StreamEvent> stream(
            AgentConfig definition,
            AgentInvocationRequest request,
            String providerMessage,
            boolean allowDraft,
            String tenantId) {
        decryptCredential(definition);
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
            case RETURN_COMPLETED -> completedStream(reservation.completedResult());
            case FINALIZE_SUCCESS -> completedStream(
                    transactions.finalizeSuccess(tenantId, invocationId, userId));
            case EXECUTE_PROVIDER -> executeProviderStream(
                    definition, request, providerMessage, userId, reservation);
        };
    }

    private Flux<StreamEvent> completedStream(AgentViews.InvocationResult result) {
        return Flux.just(StreamEvent.start(result.invocationId(), result.sessionId()), StreamEvent.complete(result));
    }

    private Flux<StreamEvent> executeProviderStream(
            AgentConfig definition,
            AgentInvocationRequest request,
            String providerMessage,
            String userId,
            AgentInvocationTransactionService.Reservation reservation) {
        long startedAt = System.nanoTime();
        AtomicBoolean settled = new AtomicBoolean();
        AtomicBoolean providerResultPersisted = new AtomicBoolean();
        AtomicBoolean providerCompleted = new AtomicBoolean();
        AgentProviderInvocation invocation = new AgentProviderInvocation(
                request,
                providerMessage,
                userId,
                reservation.invocationId(),
                reservation.providerConversationId(),
                reservation.history()
        );
        Flux<StreamEvent> providerEvents = Flux.defer(() -> providers.require(definition.getProviderType())
                        .stream(definition, invocation))
                .subscribeOn(Schedulers.boundedElastic())
                .takeUntil(event -> event.type() == AgentProviderStreamEvent.Type.COMPLETE)
                .publishOn(Schedulers.boundedElastic())
                .map(event -> mapProviderEvent(
                        event, reservation, userId, startedAt,
                        providerResultPersisted, providerCompleted, settled))
                .concatWith(Flux.defer(() -> providerCompleted.get()
                        ? Flux.empty()
                        : Flux.error(new BizException("智能体上游流式响应未正常完成"))))
                .onErrorResume(error -> {
                    RuntimeException exception = runtimeException(error);
                    if (!providerResultPersisted.get() && settled.compareAndSet(false, true)) {
                        failReservation(reservation, exception, startedAt);
                    }
                    return Flux.just(StreamEvent.error(
                            reservation.invocationId(), reservation.sessionId(), PROVIDER_FAILURE_MESSAGE));
                });
        return Flux.concat(
                Flux.just(StreamEvent.start(reservation.invocationId(), reservation.sessionId())),
                providerEvents
        ).doOnCancel(() -> releaseCancelledReservation(reservation, startedAt, settled));
    }

    private StreamEvent mapProviderEvent(
            AgentProviderStreamEvent event,
            AgentInvocationTransactionService.Reservation reservation,
            String userId,
            long startedAt,
            AtomicBoolean providerResultPersisted,
            AtomicBoolean providerCompleted,
            AtomicBoolean settled) {
        return switch (event.type()) {
            case DELTA -> StreamEvent.delta(
                    reservation.invocationId(), reservation.sessionId(), event.content());
            case REPLACE -> StreamEvent.replace(
                    reservation.invocationId(), reservation.sessionId(), event.content());
            case COMPLETE -> {
                AgentProviderResult result = event.result();
                if (result == null) throw new BizException("智能体上游完成事件缺少结果");
                transactions.recordProviderSuccess(reservation, result, elapsedMillis(startedAt));
                // 上游结果一旦落库，只能由同一幂等调用恢复完成，不能再回写为失败。
                providerResultPersisted.set(true);
                settled.set(true);
                AgentViews.InvocationResult completed = transactions.finalizeSuccess(
                        reservation.tenantId(), reservation.invocationId(), userId);
                providerCompleted.set(true);
                yield StreamEvent.complete(completed);
            }
        };
    }

    private void releaseCancelledReservation(
            AgentInvocationTransactionService.Reservation reservation,
            long startedAt,
            AtomicBoolean settled) {
        if (!settled.compareAndSet(false, true)) return;
        MonoRelease.run(() -> failReservation(
                reservation, new CancellationException("客户端已取消智能体流式调用"), startedAt));
    }

    private RuntimeException runtimeException(Throwable error) {
        if (error instanceof RuntimeException runtime) return runtime;
        return new BizException(PROVIDER_FAILURE_MESSAGE);
    }

    private AgentViews.InvocationResult invoke(
            AgentConfig definition,
            AgentInvocationRequest request,
            String providerMessage,
            boolean allowDraft,
            String tenantId) {
        decryptCredential(definition);
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
            case EXECUTE_PROVIDER -> executeProvider(
                    definition, request, providerMessage, userId, reservation);
        };
    }

    /** 普通 Spring Data JPA 查询不会触发 JpaPlusExecutor 的查询后处理，调用前显式解密版本化凭据。 */
    private void decryptCredential(AgentConfig definition) {
        String credential = definition.getCredential();
        if (StringUtils.hasText(credential) && credential.matches("^v\\d+:.+$")) {
            fieldEngine.afterQuery(definition, AgentConfig.class);
        }
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
            String providerMessage,
            String userId,
            AgentInvocationTransactionService.Reservation reservation) {
        long startedAt = System.nanoTime();
        AgentProviderResult result;
        try {
            result = providers.require(definition.getProviderType())
                    .invoke(definition, new AgentProviderInvocation(
                            request,
                            providerMessage,
                            userId,
                            reservation.invocationId(),
                            reservation.providerConversationId(),
                            reservation.history()
                    ));
        } catch (RuntimeException exception) {
            failReservation(reservation, exception, startedAt);
            throw new BizException(PROVIDER_FAILURE_MESSAGE);
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

    private long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    /**
     * 会话流式调用的内部稳定事件。
     *
     * @param type 事件类型
     * @param invocationId 调用幂等标识
     * @param sessionId 会话标识
     * @param content 增量或替换文本
     * @param result 完成后的持久化调用结果
     * @param message 错误说明
     */
    public record StreamEvent(
            Type type,
            String invocationId,
            String sessionId,
            String content,
            AgentViews.InvocationResult result,
            String message
    ) {
        /** 会话流式事件类型。 */
        public enum Type {
            /** 已完成调用预留。 */
            START,
            /** 新增回答片段。 */
            DELTA,
            /** 覆盖当前回答全文。 */
            REPLACE,
            /** 调用和持久化均已完成。 */
            COMPLETE,
            /** 调用未完成；失败已收敛，或已持久化上游结果等待同幂等调用恢复完成。 */
            ERROR
        }

        /** 创建开始事件。 */
        public static StreamEvent start(String invocationId, String sessionId) {
            return new StreamEvent(Type.START, invocationId, sessionId, null, null, null);
        }

        /** 创建增量事件。 */
        public static StreamEvent delta(String invocationId, String sessionId, String content) {
            return new StreamEvent(Type.DELTA, invocationId, sessionId, content, null, null);
        }

        /** 创建全文替换事件。 */
        public static StreamEvent replace(String invocationId, String sessionId, String content) {
            return new StreamEvent(Type.REPLACE, invocationId, sessionId, content, null, null);
        }

        /** 创建完成事件。 */
        public static StreamEvent complete(AgentViews.InvocationResult result) {
            return new StreamEvent(Type.COMPLETE, result.invocationId(), result.sessionId(), null, result, null);
        }

        /** 创建失败事件。 */
        public static StreamEvent error(String invocationId, String sessionId, String message) {
            return new StreamEvent(Type.ERROR, invocationId, sessionId, null, null, message);
        }
    }

    private static final class MonoRelease {
        private MonoRelease() {
        }

        private static void run(Runnable action) {
            reactor.core.publisher.Mono.fromRunnable(action)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
        }
    }
}
