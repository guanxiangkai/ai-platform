package com.ai.system.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 租户业务适配配置。
 *
 * <p>平台不依赖任何产品业务模块，只保存租户到业务服务名的路由关系。未配置业务适配器的租户
 * 默认关闭依赖员工档案的自助注册流程。</p>
 *
 * @param tenants 租户 ID 到业务适配器的映射
 */
@ConfigurationProperties(prefix = "platform.workforce")
public record TenantWorkforceProperties(Map<String, TenantAdapter> tenants) {

    public TenantWorkforceProperties {
        tenants = tenants == null ? Map.of() : Map.copyOf(tenants);
    }

    /**
     * 获取当前租户已启用的员工档案适配器。
     *
     * @param tenantId 租户 ID
     * @return 已验证的适配器配置
     */
    public TenantAdapter requireAdapter(String tenantId) {
        TenantAdapter adapter = tenants.get(tenantId);
        if (adapter == null || !adapter.registrationEnabled() || !StringUtils.hasText(adapter.serviceName())) {
            throw new IllegalStateException("当前租户未启用员工档案注册适配器");
        }
        return adapter;
    }

    /**
     * 单个租户的员工档案适配器。
     *
     * @param serviceName Nacos 服务名
     * @param registrationEnabled 是否启用员工档案注册
     */
    public record TenantAdapter(String serviceName, boolean registrationEnabled) {
    }
}
