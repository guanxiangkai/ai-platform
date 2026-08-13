package com.ai.scheduler.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** 手工触发任务时可覆盖的单次实例参数。 */
public record SchedulerRunRequest(
        @Size(max = 8192)
        @Schema(description = "本次执行参数；为空时使用任务默认参数")
        String parameters
) {
}
