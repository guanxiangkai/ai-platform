package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.log.entity.BaseLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 操作日志实体。
 *
 * <p>继承 {@link BaseLog} 复用租户、审计与日志公共字段，通过
 * {@code OperationLogHandler} SPI 持久化。Web Plus {@code OperationLogAspect}
 * 拦截 {@code @OperationLog} 标注的方法并生成日志。</p>
 *
 * <h3>aspect 自动填充字段</h3>
 * <ul>
 *   <li>BaseLog 公共字段：traceId / userId / username / clientIp / location / status / message / logTime</li>
 *   <li>{@code operationId}       — 本次操作全局唯一 UUID，可与数据变更日志关联</li>
 *   <li>{@code module}            — 来自 {@code @OperationLog#module()}</li>
 *   <li>{@code operationTypeCode} — 来自 {@code @OperationLog#typeCode()}</li>
 *   <li>{@code description}       — 来自 {@code @OperationLog#description()}（支持 SpEL）</li>
 *   <li>{@code requestMethod}     — HTTP 请求方法</li>
 *   <li>{@code requestUrl}        — 请求路径</li>
 *   <li>{@code requestParams}     — 请求参数（{@code saveRequestParams=true} 时记录）</li>
 *   <li>{@code responseData}      — 响应结果（{@code saveResponseData=true} 时记录）</li>
 *   <li>{@code userAgent}         — 请求 User-Agent</li>
 *   <li>{@code costMs}            — 执行耗时（毫秒）</li>
 *   <li>{@code errorMessage}      — 异常信息（执行失败时）</li>
 * </ul>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_operation_log", comment = "操作日志表", indexes = {
        @Index(name = "idx_oper_log_username", columnList = "username"),
        @Index(name = "idx_oper_log_module", columnList = "module"),
        @Index(name = "idx_oper_log_time", columnList = "log_time")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_sys_operation_log_operation_id",
        columnNames = "operation_id"
))
public class OperationLog extends BaseLog {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 本次操作全局唯一 UUID
     * <p>可与 jpa-plus DataChangeLogEntity 的 operationId 字段关联，实现完整审计链路</p>
     */
    @Column(name = "operation_id", length = 64, nullable = false, comment = "操作ID(UUID)")
    private String operationId;

    /**
     * 所属模块（如 "用户管理"、"角色管理"）
     */
    @Column(name = "module", length = 100, comment = "所属模块")
    private String module;

    /**
     * 操作类型编码（如 QUERY / INSERT / UPDATE / DELETE，支持业务自定义）。
     *
     * <p>由 Web Plus 根据 {@code @OperationLog#typeCode()} 写入。</p>
     */
    @Column(name = "operation_type", length = 50, comment = "操作类型")
    private String operationTypeCode;

    /**
     * 操作描述（支持 SpEL 解析）
     */
    @Column(name = "description", length = 500, comment = "操作描述")
    private String description;

    /**
     * HTTP 请求方法（GET / POST / PUT / DELETE 等）
     */
    @Column(name = "request_method", length = 10, comment = "HTTP请求方法")
    private String requestMethod;

    /**
     * 请求路径
     */
    @Column(name = "request_url", length = 500, comment = "请求URL")
    private String requestUrl;

    /**
     * 请求参数（JSON 序列化，saveRequestParams=true 时写入）
     */
    @Column(name = "request_params", columnDefinition = "TEXT", comment = "请求参数JSON")
    private String requestParams;

    /**
     * 响应结果（JSON 序列化，saveResponseData=true 时写入）
     */
    @Column(name = "response_data", columnDefinition = "TEXT", comment = "响应结果JSON")
    private String responseData;

    /**
     * 客户端 User-Agent
     */
    @Column(name = "user_agent", length = 500, comment = "客户端代理信息")
    private String userAgent;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "cost_ms", comment = "执行耗时(ms)")
    private Long costMs;

    /**
     * 异常信息（执行成功时为 null）
     */
    @Column(name = "error_message", length = 2000, comment = "错误信息")
    private String errorMessage;

}
