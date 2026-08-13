package com.ai.scheduler.web;

import com.ai.scheduler.domain.ScheduleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 创建或更新通用定时任务时的期望配置。 */
@Schema(description = "通用定时任务配置")
public record SchedulerTaskRequest(
        @NotBlank
        @Size(max = 128)
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_.-]{2,127}", message = "任务编码需以字母开头，且只能包含字母、数字、点、下划线和短横线")
        @Schema(description = "租户内唯一任务编码")
        String taskCode,

        @NotBlank
        @Size(max = 256)
        @Schema(description = "任务名称")
        String taskName,

        @NotBlank
        @Size(max = 64)
        @Schema(description = "应用逻辑编码")
        String applicationCode,

        @NotBlank
        @Size(max = 256)
        @Schema(description = "业务服务内的处理器 Bean 名称")
        String processorInfo,

        @NotNull
        @Schema(description = "调度类型")
        ScheduleType timeExpressionType,

        @NotBlank
        @Size(max = 256)
        @Schema(description = "Cron 或毫秒间隔表达式")
        String timeExpression,

        @Size(max = 8192)
        @Schema(description = "任务参数")
        String jobParameters,

        @Schema(description = "最大并存实例数，0 表示不限制")
        Integer maxInstanceNum,

        @Schema(description = "单实例并发线程数")
        Integer concurrency,

        @Schema(description = "单实例最长执行毫秒数，0 表示不限制")
        Long instanceTimeLimit,

        @Schema(description = "实例重试次数")
        Integer instanceRetryNum,

        @Schema(description = "任务重试次数")
        Integer taskRetryNum,

        @Schema(description = "是否启用")
        Boolean enabled,

        @Size(max = 500)
        @Schema(description = "任务说明")
        String remark
) {
}
