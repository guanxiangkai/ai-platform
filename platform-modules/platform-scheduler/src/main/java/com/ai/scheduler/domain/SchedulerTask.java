package com.ai.scheduler.domain;

import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 租户内由平台统一治理的定时任务定义。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "scheduler_task", comment = "平台通用调度任务定义")
public class SchedulerTask extends DataTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 最近同步结果的持久化字符数上限。 */
    public static final int LAST_SYNC_MESSAGE_MAX_LENGTH = 1_000;

    /** 租户内稳定且唯一的任务编码。 */
    @Column(name = "task_code", nullable = false, length = 128, comment = "任务编码")
    private String taskCode;

    /** 任务显示名称。 */
    @Column(name = "task_name", nullable = false, length = 256, comment = "任务名称")
    private String taskName;

    /** Nacos 中配置的应用逻辑编码。 */
    @Column(name = "application_code", nullable = false, length = 64, comment = "Nacos应用逻辑编码")
    private String applicationCode;

    /** 同步时使用的 PowerJob 应用名称快照。 */
    @Column(name = "application_name", nullable = false, length = 128, comment = "PowerJob应用名称快照")
    private String applicationName;

    /** 业务服务内的 PowerJob 处理器 Bean 名称。 */
    @Column(name = "processor_info", nullable = false, length = 256, comment = "PowerJob处理器Bean名称")
    private String processorInfo;

    /** 时间表达式类型。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "time_expression_type", nullable = false, length = 32, comment = "时间表达式类型")
    private ScheduleType timeExpressionType;

    /** Cron 或毫秒间隔表达式。 */
    @Column(name = "time_expression", nullable = false, length = 256, comment = "Cron或毫秒间隔表达式")
    private String timeExpression;

    /** 传给业务处理器的任务参数。 */
    @Column(name = "job_parameters", columnDefinition = "text", comment = "任务处理器参数")
    private String jobParameters;

    /** 同一任务允许并存的最大实例数，0 表示不限制。 */
    @Column(name = "max_instance_num", nullable = false, comment = "最大并存任务实例数")
    private Integer maxInstanceNum = 1;

    /** 单实例并发线程数。 */
    @Column(name = "concurrency", nullable = false, comment = "单实例并发线程数")
    private Integer concurrency = 1;

    /** 单实例最长执行时间，单位毫秒；0 表示不限制。 */
    @Column(name = "instance_time_limit", nullable = false, comment = "单实例最长执行时间毫秒数")
    private Long instanceTimeLimit = 0L;

    /** 实例级重试次数。 */
    @Column(name = "instance_retry_num", nullable = false, comment = "实例级重试次数")
    private Integer instanceRetryNum = 0;

    /** 任务级重试次数。 */
    @Column(name = "task_retry_num", nullable = false, comment = "任务级重试次数")
    private Integer taskRetryNum = 0;

    /** PowerJob 中的任务标识。 */
    @Column(name = "powerjob_job_id", comment = "PowerJob任务标识")
    private Long powerjobJobId;

    /** 本地定义与 PowerJob 的同步状态。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_state", nullable = false, length = 20, comment = "与PowerJob同步状态")
    private SchedulerSyncState syncState = SchedulerSyncState.PENDING;

    /** 最近一次同步时间。 */
    @Column(name = "last_sync_time", comment = "最近同步时间")
    private LocalDateTime lastSyncTime;

    /** 最近一次同步结果或失败原因。 */
    @Column(name = "last_sync_message", length = LAST_SYNC_MESSAGE_MAX_LENGTH,
            comment = "最近同步结果或失败原因")
    private String lastSyncMessage;
}
