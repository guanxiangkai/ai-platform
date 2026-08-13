package com.ai.api.context;

import java.lang.ScopedValue;
import java.util.Objects;

/**
 * 后台任务和内部调用使用的租户执行作用域。
 *
 * <p>请求链路仍以认证上下文中的租户为准；只有消息消费、异步日志和管理任务等
 * 不具备登录用户的同步执行链路，才应在最小代码块内绑定租户。基于 Java 25
 * {@link ScopedValue} 实现，嵌套调用会自动恢复外层值，也不会因异常造成 ThreadLocal 泄漏。</p>
 */
public final class TenantExecutionScope {

    private static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();

    private TenantExecutionScope() {
    }

    /**
     * 获取当前后台执行作用域中的租户标识。
     *
     * @return 租户标识；当前没有显式作用域时返回 {@code null}
     */
    public static String currentTenantId() {
        return TENANT_ID.isBound() ? TENANT_ID.get() : null;
    }

    /**
     * 在指定租户作用域中执行同步任务。
     *
     * @param tenantId 租户标识，不能为空
     * @param action 同步任务；方法返回或抛出异常后都会自动恢复外层作用域
     * @throws IllegalArgumentException 租户标识为空时抛出
     * @throws NullPointerException 任务为空时抛出
     */
    public static void run(String tenantId, Runnable action) {
        Objects.requireNonNull(action, "action must not be null");
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        ScopedValue.where(TENANT_ID, tenantId.trim()).run(action);
    }
}
