package com.ai.api.agent.config;

import com.ai.api.agent.AgentApiHeaders;
import com.ai.api.agent.client.AgentClient;
import com.ai.api.agent.dto.AgentInvokeRequest;
import com.ai.api.agent.dto.AgentInvokeResult;
import com.ai.api.context.TenantExecutionScope;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;

/**
 * 基于 Nacos 服务发现的通用 Agent 客户端自动配置。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration",
        "org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerClientAutoConfiguration"
})
@EnableConfigurationProperties(TrustedForwardProperties.class)
public class AgentClientConfig {

    /** 创建携带可信身份与租户上下文的通用 Agent 客户端。 */
    @Bean
    @ConditionalOnMissingBean(AgentClient.class)
    public AgentClient agentClient(
            LoadBalancedExchangeFilterFunction loadBalancer,
            TrustedForwardProperties trustedForward,
            WebClient.Builder webClientBuilder) {
        trustedForward.validateConfigured("platform-agent 内部客户端");
        WebClient webClient = webClientBuilder.clone()
                .baseUrl("http://platform-agent")
                .defaultHeader(trustedForward.getHeaderName(), trustedForward.getToken())
                .filter(tenantForwardingFilter())
                .filter(loadBalancer)
                .build();
        InternalAgentClient proxy = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build()
                .createClient(InternalAgentClient.class);

        return (agentCode, idempotencyKey, request) -> proxy.invoke(
                currentUserId(),
                agentCode,
                requireIdempotencyKey(idempotencyKey),
                request);
    }

    private static ExchangeFilterFunction tenantForwardingFilter() {
        return (request, next) -> {
            String tenantId = SecurityUtils.getTenantId();
            if (!StringUtils.hasText(tenantId)) {
                tenantId = TenantExecutionScope.currentTenantId();
            }
            if (!StringUtils.hasText(tenantId)) {
                return next.exchange(request);
            }
            String resolvedTenantId = tenantId.trim();
            ClientRequest forwarded = ClientRequest.from(request)
                    .headers(headers -> headers.set(
                            AuthConstants.HeaderConstants.TENANT_ID,
                            resolvedTenantId
                    ))
                    .build();
            return next.exchange(forwarded);
        };
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
    private interface InternalAgentClient {
        @PostExchange("/definitions/{agentCode}/invoke")
        Mono<AgentInvokeResult> invoke(
                @RequestHeader(AuthConstants.HeaderConstants.USER_ID) String userId,
                @PathVariable String agentCode,
                @RequestHeader(AgentApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
                @RequestBody AgentInvokeRequest request
        );
    }
}
