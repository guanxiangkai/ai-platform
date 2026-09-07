package com.ai.system.integration;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;

/**
 * 租户外部目录适配配置。
 *
 * <p>平台只保存租户到目录服务名的路由关系；未配置或未启用适配器的租户不得调用其他租户的目录。</p>
 *
 * @param tenants 租户 ID 到目录适配器的映射
 * @param requestTimeout 单次目录 HTTP 调用的响应超时
 */
@ConfigurationProperties(prefix = "platform.directory")
public record TenantDirectoryProperties(
        Map<String, TenantAdapter> tenants,
        @DefaultValue("5s") Duration requestTimeout
) {

    /** 目录调用默认响应超时。 */
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(5);

    @ConstructorBinding
    public TenantDirectoryProperties {
        tenants = tenants == null ? Map.of() : Map.copyOf(tenants);
        requestTimeout = requestTimeout == null ? DEFAULT_REQUEST_TIMEOUT : requestTimeout;
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("平台目录请求超时必须为正时长");
        }
    }

    /** 使用默认超时创建目录配置，供非配置绑定场景使用。 */
    public TenantDirectoryProperties(Map<String, TenantAdapter> tenants) {
        this(tenants, DEFAULT_REQUEST_TIMEOUT);
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
