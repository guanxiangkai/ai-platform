package com.ai.system.config;

import com.ai.api.context.TenantExecutionScope;
import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

/**
 * 平台系统服务的租户上下文配置。
 *
 * <p>普通 HTTP 请求读取网关认证上下文；后台任务可在最小同步代码块内使用
 * {@link TenantExecutionScope} 显式绑定租户。两种来源最终统一提供给 JPA Plus，
 * 保证查询过滤和新增实体租户字段使用同一权威值。</p>
 */
@Configuration(proxyBeanMethods = false)
public class TenantContextConfiguration {

    /**
     * 创建平台租户标识提供器。
     *
     * @return 优先读取后台执行作用域、其次读取请求认证上下文的租户提供器
     */
    @Bean
    @Primary
    public TenantIdProvider platformTenantIdProvider() {
        return () -> {
            String scopedTenantId = TenantExecutionScope.currentTenantId();
            return StringUtils.hasText(scopedTenantId) ? scopedTenantId : SecurityUtils.getTenantId();
        };
    }
}
