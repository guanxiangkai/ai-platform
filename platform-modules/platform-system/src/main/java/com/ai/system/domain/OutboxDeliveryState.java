package com.ai.system.domain;

/**
 * 事务外投递任务的处理状态。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum OutboxDeliveryState {
    PENDING,
    PROCESSING,
    RETRY,
    SENT,
    DEAD
}
