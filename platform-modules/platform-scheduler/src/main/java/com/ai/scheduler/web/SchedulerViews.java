package com.ai.scheduler.web;

import com.ai.scheduler.domain.ScheduleType;
import com.ai.scheduler.domain.SchedulerSyncState;
import com.ai.scheduler.domain.SchedulerTask;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/** 通用调度管理页面使用的稳定视图契约。 */
public final class SchedulerViews {

    private SchedulerViews() {
    }

    /** 可配置的业务处理器。 */
    public record Handler(
            String processorInfo,
            String displayName,
            String description
    ) {
    }

    /** 当前租户可使用的 PowerJob 应用。 */
    public record Application(
            String code,
            String displayName,
            String appName,
            List<Handler> handlers
    ) {
    }

    /** 调度任务详情及同步状态。 */
    @Schema(description = "通用调度任务")
    public record Task(
            String id,
            String taskCode,
            String taskName,
            String applicationCode,
            String applicationName,
            String processorInfo,
            ScheduleType timeExpressionType,
            String timeExpression,
            String jobParameters,
            Integer maxInstanceNum,
            Integer concurrency,
            Long instanceTimeLimit,
            Integer instanceRetryNum,
            Integer taskRetryNum,
            Boolean enabled,
            String remark,
            Long powerjobJobId,
            SchedulerSyncState syncState,
            LocalDateTime lastSyncTime,
            String lastSyncMessage,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
        /** 从租户内任务实体生成对外视图。 */
        public static Task from(SchedulerTask task) {
            return new Task(
                    task.getId(), task.getTaskCode(), task.getTaskName(),
                    task.getApplicationCode(), task.getApplicationName(), task.getProcessorInfo(),
                    task.getTimeExpressionType(), task.getTimeExpression(), task.getJobParameters(),
                    task.getMaxInstanceNum(), task.getConcurrency(), task.getInstanceTimeLimit(),
                    task.getInstanceRetryNum(), task.getTaskRetryNum(), task.getEnabled(), task.getRemark(),
                    task.getPowerjobJobId(), task.getSyncState(), task.getLastSyncTime(),
                    task.getLastSyncMessage(), task.getCreateTime(), task.getUpdateTime());
        }
    }

    /** PowerJob 执行实例的只读视图。 */
    public record Instance(
            Long instanceId,
            Long jobId,
            Integer status,
            String statusCode,
            String statusLabel,
            String jobParameters,
            String instanceParameters,
            String result,
            Instant expectedTriggerTime,
            Instant actualTriggerTime,
            Instant finishedTime,
            Long runningTimes,
            Instant createTime,
            Instant updateTime
    ) {
    }
}
