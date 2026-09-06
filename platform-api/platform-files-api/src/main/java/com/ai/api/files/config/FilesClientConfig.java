package com.ai.api.files.config;

import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import com.ai.api.context.TenantExecutionScope;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import com.ai.api.files.client.FilesClient;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 平台文件服务内部客户端自动配置。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration",
        "org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerClientAutoConfiguration"
})
@EnableConfigurationProperties({TrustedForwardProperties.class, FilesClientProperties.class})
public class FilesClientConfig {

    /**
     * 创建基于 Nacos 服务发现的文件客户端。
     *
     * @param lbFunction 负载均衡过滤器
     * @param trustedForwardProperties 内部可信转发配置
     * @param filesClientProperties 文件服务调用时限
     * @param webClientBuilder WebClient 构建器
     * @return 文件服务客户端
     */
    @Bean
    @ConditionalOnBean({LoadBalancedExchangeFilterFunction.class, WebClient.Builder.class})
    @ConditionalOnMissingBean(FilesClient.class)
    public FilesClient filesClient(LoadBalancedExchangeFilterFunction lbFunction,
                                   TrustedForwardProperties trustedForwardProperties,
                                   FilesClientProperties filesClientProperties,
                                   WebClient.Builder webClientBuilder) {
        trustedForwardProperties.validateConfigured("platform-files 内部客户端");

        WebClient webClient = webClientBuilder.clone()
                .baseUrl("http://platform-files")
                .defaultHeader(AuthConstants.HeaderConstants.USER_ID,
                        AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID)
                .defaultHeader(trustedForwardProperties.getHeaderName(), trustedForwardProperties.getToken())
                .filter(tenantForwardingFilter())
                .filter(lbFunction)
                .build();

        return new HttpFilesClient(webClient, filesClientProperties);
    }

    private static ExchangeFilterFunction tenantForwardingFilter() {
        return (request, next) -> {
            if (request.headers().getFirst(AuthConstants.HeaderConstants.TENANT_ID) != null) {
                return next.exchange(request);
            }
            String tenantId = SecurityUtils.getTenantId();
            if (!StringUtils.hasText(tenantId)) {
                tenantId = TenantExecutionScope.currentTenantId();
            }
            if (!StringUtils.hasText(tenantId)) {
                return next.exchange(request);
            }
            ClientRequest tenantRequest = ClientRequest.from(request)
                    .header(AuthConstants.HeaderConstants.TENANT_ID, tenantId)
                    .build();
            return next.exchange(tenantRequest);
        };
    }

}
