package com.ai.system.integration;

import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 按租户路由到业务侧人员档案适配器。
 *
 * <p>业务服务名来自配置，平台只依赖稳定的 HTTP 契约，不引用任何产品代码。</p>
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(TenantWorkforceProperties.class)
public class TenantWorkforceClient {

    private final TenantWorkforceProperties properties;
    private final TenantIdProvider tenantIdProvider;
    private final TrustedForwardProperties trustedForwardProperties;
    private final LoadBalancedExchangeFilterFunction loadBalancer;
    private final WebClient.Builder webClientBuilder;

    /**
     * 匹配当前租户的人员档案。
     */
    public Mono<WorkforceMatchResult> matchForRegister(
            String realName,
            String deptId,
            String phone,
            String email
    ) {
        return client().get()
                .uri(uriBuilder -> uriBuilder.path("/internal/personnel/matchForRegister")
                        .queryParam("realName", realName)
                        .queryParam("deptId", deptId)
                        .queryParamIfPresent("phone", optionalText(phone))
                        .queryParamIfPresent("email", optionalText(email))
                        .build())
                .retrieve()
                .bodyToMono(WorkforceMatchResult.class);
    }

    /**
     * 将平台账户绑定到当前租户的人员档案。
     */
    public Mono<Boolean> bindUser(String personnelId, String userId) {
        return client().post()
                .uri(uriBuilder -> uriBuilder.path("/internal/personnel/bindUser")
                        .queryParam("personnelId", personnelId)
                        .queryParam("userId", userId)
                        .build())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(Boolean.class);
    }

    /**
     * 查询当前租户人员的在岗职位。
     */
    public Mono<WorkforcePosition> currentPosition(String personnelId) {
        return client().get()
                .uri("/internal/personnel/{personnelId}/currentPosition", personnelId)
                .retrieve()
                .bodyToMono(WorkforcePosition.class);
    }

    private WebClient client() {
        String tenantId = tenantIdProvider.getCurrentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("当前请求缺少租户标识");
        }
        TenantWorkforceProperties.TenantAdapter adapter = properties.requireAdapter(tenantId);
        trustedForwardProperties.validateConfigured("平台租户业务适配客户端");
        return webClientBuilder.clone()
                .baseUrl("http://" + adapter.serviceName().trim())
                .defaultHeader(AuthConstants.HeaderConstants.USER_ID,
                        AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID)
                .defaultHeader(AuthConstants.HeaderConstants.TENANT_ID, tenantId)
                .defaultHeader(trustedForwardProperties.getHeaderName(), trustedForwardProperties.getToken())
                .filter(loadBalancer)
                .build();
    }

    private java.util.Optional<String> optionalText(String value) {
        return StringUtils.hasText(value) ? java.util.Optional.of(value.trim()) : java.util.Optional.empty();
    }
}
