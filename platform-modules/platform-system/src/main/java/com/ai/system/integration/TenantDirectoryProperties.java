package com.ai.system.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 租户外部目录适配配置。
 *
 * <p>平台只保存租户到目录服务名的路由关系；未配置适配器的租户默认关闭目录匹配注册。</p>
 *
 * @param tenants 租户 ID 到目录适配器的映射
 */
@ConfigurationProperties(prefix = "platform.directory")
public record TenantDirectoryProperties(Map<String, TenantAdapter> tenants) {

    public TenantDirectoryProperties {
        tenants = tenants == null ? Map.of() : Map.copyOf(tenants);
    }

    /**
     * 获取当前租户启用的目录适配器。
     *
     * @param tenantId 租户 ID
     * @return 已验证的适配器配置
     */
    public TenantAdapter requireAdapter(String tenantId) {
        TenantAdapter adapter = tenants.get(tenantId);
        if (adapter == null || !adapter.registrationEnabled() || !StringUtils.hasText(adapter.serviceName())) {
            throw new IllegalStateException("当前租户未启用目录注册适配器");
        }
        return adapter;
    }

    /**
     * 单个租户的目录适配器。
     *
     * @param serviceName 服务发现名称
     * @param registrationEnabled 是否启用目录匹配注册
     */
    public record TenantAdapter(String serviceName, boolean registrationEnabled) {
    }
}
