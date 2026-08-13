package com.ai.system.domain;

/**
 * 注册申请的唯一生命周期状态。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum RegisterState {
    PENDING,
    REJECTED,
    PROVISIONING,
    PROVISIONING_FAILED,
    ACTIVE
}
