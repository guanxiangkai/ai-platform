package com.ai.system.integration;

import com.ai.system.integration.TenantDirectoryProperties.TenantAdapter;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 按租户路由到外部目录适配器。
 *
 * <p>服务名来自受控运行配置，平台只依赖稳定的 HTTP 契约，不引用任何消费方实现。</p>
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(TenantDirectoryProperties.class)
public class TenantDirectoryClient {

    private final TenantDirectoryProperties properties;
    private final TenantIdProvider tenantIdProvider;
    private final TrustedForwardProperties trustedForwardProperties;
    private final LoadBalancedExchangeFilterFunction loadBalancer;
    private final WebClient.Builder webClientBuilder;

    /**
     * 在当前租户的目录中匹配注册主体。
     *
     * @param displayName 注册人显示名称
     * @param groupId 平台部门 ID；由当前租户的目录适配器转换为其外部目录分组
     * @param phone 注册手机号
     * @param email 注册邮箱
     * @return 当前租户目录返回的匹配结果
     */
    public Mono<DirectoryMatchResult> matchForRegistration(
            String displayName,
            String groupId,
            String phone,
            String email
    ) {
        Map<String, String> uriVariables = new HashMap<>();
        Optional<String> phoneValue = optionalText(phone);
        Optional<String> emailValue = optionalText(email);
        return client().get()
                .uri(uriBuilder -> {
                    Optional<String> displayNameTemplate = queryTemplate(uriVariables, "displayName", displayName);
                    Optional<String> groupIdTemplate = queryTemplate(uriVariables, "groupId", groupId);
                    Optional<String> phoneTemplate = phoneValue.flatMap(value -> queryTemplate(uriVariables, "phone", value));
                    Optional<String> emailTemplate = emailValue.flatMap(value -> queryTemplate(uriVariables, "email", value));
                    return uriBuilder.path("/internal/directory/matchForRegistration")
                            .queryParamIfPresent("displayName", displayNameTemplate)
                            .queryParamIfPresent("groupId", groupIdTemplate)
                            .queryParamIfPresent("phone", phoneTemplate)
                            .queryParamIfPresent("email", emailTemplate)
                            .build(uriVariables);
                })
                .retrieve()
                .bodyToMono(DirectoryMatchResult.class)
                .timeout(properties.requestTimeout());
    }

    /**
     * 将平台账户绑定到当前租户的目录主体。
     */
    public Mono<Boolean> linkUser(String subjectId, String userId) {
        DirectoryMatchResult.requireIdentityId(subjectId, "目录主体 ID");
        DirectoryMatchResult.requireIdentityId(userId, "平台用户 ID");
        Map<String, String> uriVariables = Map.of("subjectId", subjectId, "userId", userId);
        return client().post()
                .uri(uriBuilder -> uriBuilder.path("/internal/directory/linkUser")
                        .queryParam("subjectId", "{subjectId}")
                        .queryParam("userId", "{userId}")
                        .build(uriVariables))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(Boolean.class)
                .timeout(properties.requestTimeout());
    }

    /**
     * 查询当前目录主体的有效分配信息。
     */
    public Mono<DirectoryAssignment> currentAssignment(String subjectId) {
        DirectoryMatchResult.requireIdentityId(subjectId, "目录主体 ID");
        return client().get()
                .uri(uriBuilder -> uriBuilder.path("/internal/directory")
                        .pathSegment("{subjectId}")
                        .path("/currentAssignment")
                        .build(subjectId))
                .retrieve()
                .bodyToMono(DirectoryAssignment.class)
                .timeout(properties.requestTimeout());
    }

    private WebClient client() {
        String tenantId = tenantIdProvider.getCurrentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("当前请求缺少租户标识");
        }
        TenantAdapter adapter = properties.requireAdapter(tenantId);
        trustedForwardProperties.validateConfigured("平台租户目录适配客户端");
        return webClientBuilder.clone()
                .baseUrl("http://" + adapter.serviceName().trim())
                .defaultHeader(AuthConstants.HeaderConstants.USER_ID,
                        AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID)
                .defaultHeader(AuthConstants.HeaderConstants.TENANT_ID, tenantId)
                .defaultHeader(trustedForwardProperties.getHeaderName(), trustedForwardProperties.getToken())
                .filter(loadBalancer)
                .build();
    }

    private Optional<String> optionalText(String value) {
        return StringUtils.hasText(value) ? java.util.Optional.of(value.trim()) : java.util.Optional.empty();
    }

    private Optional<String> queryTemplate(Map<String, String> uriVariables, String name, String value) {
        if (value == null) {
            return Optional.empty();
        }
        uriVariables.put(name, value);
        return Optional.of("{" + name + "}");
    }
}
