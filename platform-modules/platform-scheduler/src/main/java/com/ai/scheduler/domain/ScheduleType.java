package com.ai.scheduler.domain;

/** 平台允许用户配置的调度时间表达式类型。 */
public enum ScheduleType {
    /** Cron 表达式。 */
    CRON,
    /** 固定频率，表达式单位为毫秒。 */
    FIXED_RATE,
    /** 固定延迟，表达式单位为毫秒。 */
    FIXED_DELAY
}
