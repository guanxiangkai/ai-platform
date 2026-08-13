package com.ai.api.system.config;

import com.ai.api.context.TenantExecutionScope;
import com.ai.api.system.client.SystemClient;
import com.ai.api.system.dto.DictDTO;
import com.ai.api.system.dto.DeptIdentityDTO;
import com.ai.api.system.dto.ImportTemplateFieldDTO;
import com.ai.api.system.dto.ImportDefinitionDTO;
import com.ai.api.system.dto.PushPreferenceBatchRequest;
import com.ai.api.system.dto.WeatherInfoDTO;
import com.ai.api.system.dto.UserPushPreferenceDTO;
import com.ai.api.system.dto.UserOrganizationDTO;
import com.ai.api.system.dto.UserIdentityBatchRequest;
import com.ai.api.system.dto.UserIdentityDTO;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.exception.ServiceUnavailableException;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * platform-system 声明式 HTTP 客户端自动配置。
 *
 * <p>所有下游调用失败均转换为保留操作名称与原始原因的
 * {@link ServiceUnavailableException}，调用方不会收到伪造的业务默认值。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration",
        "org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerClientAutoConfiguration"
})
@EnableConfigurationProperties(TrustedForwardProperties.class)
public class SystemClientConfig {

    @Bean
    @ConditionalOnBean({LoadBalancedExchangeFilterFunction.class, WebClient.Builder.class})
    @ConditionalOnMissingBean(SystemClient.class)
    public SystemClient systemClient(LoadBalancedExchangeFilterFunction lbFunction,
                                     TrustedForwardProperties trustedForwardProperties,
                                     WebClient.Builder webClientBuilder) {

        trustedForwardProperties.validateConfigured("platform-system 内部客户端");

        WebClient webClient = webClientBuilder.clone()
                .baseUrl("http://platform-system")
                .defaultHeader(AuthConstants.HeaderConstants.USER_ID, AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID)
                .defaultHeader(trustedForwardProperties.getHeaderName(), trustedForwardProperties.getToken())
                .filter(tenantForwardingFilter())
                .filter(lbFunction)
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build();

        SystemClient proxy = factory.createClient(SystemClient.class);

        log.info("platform-system HTTP Interface client initialized");

        return new SystemClient() {
            @Override
            public Mono<List<UserPushPreferenceDTO>> getPushPreferencesForTenant(
                    String tenantId, PushPreferenceBatchRequest request) {
                return propagateFailure(proxy.getPushPreferencesForTenant(tenantId, request),
                        "getPushPreferencesForTenant");
            }

            @Override
            public Mono<List<DictDTO>> getDictByType(String type) {
                return propagateFailure(proxy.getDictByType(type), "getDictByType");
            }

            @Override
            public Mono<List<ImportTemplateFieldDTO>> getImportTemplateFieldByModule(String module) {
                return propagateFailure(proxy.getImportTemplateFieldByModule(module),
                        "getImportTemplateFieldByModule");
            }

            @Override
            public Mono<List<ImportDefinitionDTO>> getEnabledImportDefinitions(String tenantId) {
                return propagateFailure(proxy.getEnabledImportDefinitions(tenantId),
                        "getEnabledImportDefinitions");
            }

            @Override
            public Mono<WeatherInfoDTO> getTodayWeatherByDept(String deptId) {
                return propagateFailure(proxy.getTodayWeatherByDept(deptId), "getTodayWeatherByDept");
            }

            @Override
            public Mono<DeptIdentityDTO> getDept(String deptId) {
                return propagateFailure(proxy.getDept(deptId), "getDept");
            }

            @Override
            public Mono<DeptIdentityDTO> getDeptForTenant(String tenantId, String deptId) {
                return propagateFailure(proxy.getDeptForTenant(tenantId, deptId), "getDeptForTenant");
            }

            @Override
            public Mono<UserOrganizationDTO> getUserOrganization(String userId) {
                return propagateFailure(proxy.getUserOrganization(userId), "getUserOrganization");
            }

            @Override
            public Mono<UserOrganizationDTO> getUserOrganizationForTenant(String tenantId, String userId) {
                return propagateFailure(proxy.getUserOrganizationForTenant(tenantId, userId),
                        "getUserOrganizationForTenant");
            }

            @Override
            public Mono<List<UserIdentityDTO>> getUsersByUsernames(UserIdentityBatchRequest request) {
                return propagateFailure(proxy.getUsersByUsernames(request), "getUsersByUsernames");
            }

            @Override
            public Mono<Boolean> isUserInDept(String userId, String deptId) {
                return propagateFailure(proxy.isUserInDept(userId, deptId), "isUserInDept");
            }

            @Override
            public Mono<Boolean> isUserInDeptForTenant(String tenantId, String userId, String deptId) {
                return propagateFailure(proxy.isUserInDeptForTenant(tenantId, userId, deptId),
                        "isUserInDeptForTenant");
            }
        };
    }

    private static <T> Mono<T> propagateFailure(Mono<T> invocation, String operation) {
        return invocation.onErrorMap(cause -> new ServiceUnavailableException(
                "platform-system（操作：" + operation + "）", cause));
    }

    static ExchangeFilterFunction tenantForwardingFilter() {
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
