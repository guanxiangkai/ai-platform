package com.ai.scheduler.domain;

/** 本地任务定义与 PowerJob 的同步状态。 */
public enum SchedulerSyncState {
    /** 等待首次同步或重新同步。 */
    PENDING,
    /** 已与 PowerJob 保持一致。 */
    SYNCED,
    /** 最近一次同步失败，本地期望配置仍被保留。 */
    FAILED
}
