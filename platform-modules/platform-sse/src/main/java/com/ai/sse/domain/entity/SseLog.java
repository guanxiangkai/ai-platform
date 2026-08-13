package com.ai.sse.domain.entity;

import com.ai.sse.domain.SseOperationType;
import io.github.guanxiangkai.web.plus.log.entity.BaseLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * SSE 推送操作日志实体。
 *
 * <p>继承 {@link BaseLog} 复用租户、审计与日志公共字段，通过
 * {@code SseLogHandler} SPI 持久化。Web Plus {@code SseLogAspect}
 * 拦截 {@code @SseLog} 标注的方法并生成日志。</p>
 *
 * <h3>aspect 自动填充字段（通过 LogEntityBinder）</h3>
 * <ul>
 *   <li>BaseLog 公共字段：traceId / userId / username / status / message / logTime</li>
 *   <li>{@code messageType} — 来自 {@code @SseLog#messageType()}</li>
 *   <li>{@code targetType}  — 来自 {@code @SseLog#targetType()}</li>
 *   <li>{@code description} — 来自 {@code @SseLog#description()}（支持 SpEL）</li>
 *   <li>{@code content}     — 推送内容（{@code saveContent=true} 时记录）</li>
 *   <li>{@code operationId} — 关联的操作 ID</li>
 *   <li>{@code costMs}      — 执行耗时（毫秒）</li>
 *   <li>{@code failReason}  — 失败原因（执行失败时）</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @SseLog(entity = SseLog.class, messageType = "NOTICE", targetType = "USER", description = "推送通知", saveContent = true)
 * public Mono<Void> pushNotice(...) { ... }
 * }</pre>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sse_op_log", comment = "SSE操作日志表", indexes = {
        @Index(name = "idx_sse_op_log_user", columnList = "user_id"),
        @Index(name = "idx_sse_op_log_time", columnList = "log_time"),
        @Index(name = "idx_sse_op_log_type", columnList = "operation_type"),
        @Index(name = "idx_sse_op_log_conn", columnList = "connection_id")
})
public class SseLog extends BaseLog {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 操作类型：CONNECT（建立连接）/ DISCONNECT（断开连接）/ PUSH（推送消息）
     * <p>区分两类 SSE 操作，方便按类型查询审计</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", length = 20, nullable = false, comment = "操作类型(CONNECT/DISCONNECT/PUSH)")
    private SseOperationType operationType;

    /**
     * SSE 连接 ID（对应 SseConnectionRecord.connectionId）
     * <p>CONNECT / DISCONNECT 类型必填，PUSH 类型可关联</p>
     */
    @Column(name = "connection_id", length = 64, comment = "连接ID")
    private String connectionId;

    /**
     * 消息类型（如 NOTICE / ALERT / REFRESH，PUSH 类型时有值）
     */
    @Column(name = "message_type", length = 50, comment = "消息类型")
    private String messageType;

    /**
     * 推送目标类型（USER / USERS / TENANT / BROADCAST）
     */
    @Column(name = "target_type", length = 20, comment = "目标类型")
    private String targetType;

    /**
     * 操作描述（支持 SpEL）
     */
    @Column(name = "description", length = 500, comment = "操作描述")
    private String description;

    /**
     * 推送内容（saveContent=true 时记录）
     */
    @Column(name = "content", columnDefinition = "TEXT", comment = "推送内容")
    private String content;

    /**
     * 关联操作 ID（可与操作日志关联）
     */
    @Column(name = "operation_id", length = 64, comment = "操作ID")
    private String operationId;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "cost_ms", comment = "执行耗时(ms)")
    private Long costMs;

    /**
     * 失败原因
     */
    @Column(name = "fail_reason", length = 500, comment = "失败原因")
    private String failReason;
}
