package com.ai.api.agent.config;

import com.ai.api.agent.AgentApiHeaders;
import com.ai.api.agent.client.AgentClient;
import com.ai.api.agent.dto.AgentInvokeRequest;
import com.ai.api.agent.dto.AgentInvokeResult;
import com.ai.api.context.TenantExecutionScope;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import io.github.guanxiangkai.web.plus.security.client.TenantForwardingExchangeFilterFunction;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerWebClientHttpServiceGroupConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;
import org.springframework.web.service.registry.ImportHttpServices;
import reactor.core.publisher.Mono;

/**
 * 基于 Nacos 服务发现的通用 Agent 客户端自动配置。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration",
        "org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerClientAutoConfiguration",
        "org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerBeanPostProcessorAutoConfiguration"
})
@AutoConfigureBefore(name =
        "org.springframework.boot.webclient.autoconfigure.service.ReactiveHttpServiceClientAutoConfiguration")
@EnableConfigurationProperties(TrustedForwardProperties.class)
@Import(AgentClientConfig.AgentHttpServiceConfiguration.class)
public class AgentClientConfig {

    static final String SERVICE_ID = "platform-agent";

    /**
     * 注册 Agent HTTP Service 组及其平台安全配置。
     *
     * <p>使用 Spring Framework 7 的 HTTP Service Registry 统一创建和 AOT 注册代理，
     * Spring Cloud LoadBalancer 根据组名 {@value SERVICE_ID} 解析 Nacos 服务实例。</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({HttpServiceProxyRegistry.class, WebClientHttpServiceGroupConfigurer.class})
    @ConditionalOnBean(LoadBalancerWebClientHttpServiceGroupConfigurer.class)
    @ConditionalOnMissingBean(AgentClient.class)
    @ImportHttpServices(
            group = SERVICE_ID,
            types = InternalAgentClient.class,
            clientType = HttpServiceGroup.ClientType.WEB_CLIENT)
    static class AgentHttpServiceConfiguration {

        /** 为 Agent HTTP Service 组配置可信转发头和租户上下文。 */
        @Bean
        WebClientHttpServiceGroupConfigurer agentHttpServiceGroupConfigurer(
                TrustedForwardProperties trustedForward) {
            trustedForward.validateConfigured("platform-agent 内部客户端");
            return groups -> groups.filterByName(SERVICE_ID).forEachClient((group, builder) -> builder
                    .defaultHeader(trustedForward.getHeaderName(), trustedForward.getToken())
                    .filter(new TenantForwardingExchangeFilterFunction(
                            TenantExecutionScope::currentTenantId)));
        }

        /** 创建携带当前用户和显式幂等键的 Agent 客户端外观。 */
        @Bean
        AgentClient agentClient(InternalAgentClient proxy) {
            return (agentCode, idempotencyKey, request) -> proxy.invoke(
                    currentUserId(),
                    agentCode,
                    requireIdempotencyKey(idempotencyKey),
                    request);
        }
    }

    private static String currentUserId() {
        String userId = SecurityUtils.getUserId();
        return StringUtils.hasText(userId)
                ? userId.trim()
                : AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID;
    }

    private static String requireIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new IllegalArgumentException("幂等键不能为空");
        }
        return idempotencyKey.trim();
    }

    @HttpExchange("/internal/agent")
    interface InternalAgentClient {
        @PostExchange("/definitions/{agentCode}/invoke")
        Mono<AgentInvokeResult> invoke(
                @RequestHeader(AuthConstants.HeaderConstants.USER_ID) String userId,
                @PathVariable String agentCode,
                @RequestHeader(AgentApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
                @RequestBody AgentInvokeRequest request
        );
    }
}
