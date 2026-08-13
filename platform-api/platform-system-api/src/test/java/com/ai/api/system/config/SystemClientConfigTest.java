package com.ai.api.system.config;

import com.ai.api.context.TenantExecutionScope;
import com.ai.api.system.client.SystemClient;
import com.ai.api.system.dto.PushPreferenceBatchRequest;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.exception.ServiceUnavailableException;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemClientConfigTest {

    @Test
    void weatherResponseShouldDecodeProjectDateTimeFormat() {
        String responseBody = """
                {
                  "weatherDate":"2026-07-14",
                  "cityCode":"101190101",
                  "cityName":"南京",
                  "collectTime":"2026-07-02 11:24:33"
                }
                """;
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(responseBody)
                        .build()))
                .build();

        com.ai.api.system.dto.WeatherInfoDTO weather = webClient.get()
                .uri("http://platform-system/internal/weather/today-by-dept?deptId=dept-1")
                .retrieve()
                .bodyToMono(com.ai.api.system.dto.WeatherInfoDTO.class)
                .block();

        assertThat(weather).isNotNull();
        assertThat(weather.collectTime()).isEqualTo(LocalDateTime.of(2026, 7, 2, 11, 24, 33));
    }

    @Test
    void tenantForwardingFilterShouldForwardCurrentUserTenant() {
        AtomicReference<ClientRequest> forwardedRequest = new AtomicReference<>();
        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("http://platform-system/internal/weather/today-by-dept")
        ).build();
        UserContextHolder.set(new UserContext(
                "user-1",
                "tenant-user",
                false,
                "dept-1",
                Set.of("dept-1"),
                Set.of("OPERATOR"),
                Set.of("system:dept:list"),
                Map.of()
        ));

        try {
            SystemClientConfig.tenantForwardingFilter()
                    .filter(request, forwarded -> {
                        forwardedRequest.set(forwarded);
                        return Mono.just(ClientResponse.create(HttpStatus.OK).build());
                    })
                    .block();
        } finally {
            UserContextHolder.clear();
        }

        assertThat(forwardedRequest.get()).isNotNull();
        assertThat(forwardedRequest.get().headers().getFirst(AuthConstants.HeaderConstants.TENANT_ID))
                .isEqualTo("tenant-user");
        assertThat(UserContextHolder.get()).isNull();
    }

    @Test
    void tenantForwardingFilterShouldForwardBackgroundTenantScope() {
        AtomicReference<ClientRequest> forwardedRequest = new AtomicReference<>();
        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("http://platform-system/internal/weather/today-by-dept")
        ).build();

        TenantExecutionScope.run("tenant-1", () -> SystemClientConfig.tenantForwardingFilter()
                .filter(request, forwarded -> {
                    forwardedRequest.set(forwarded);
                    return Mono.just(ClientResponse.create(HttpStatus.OK).build());
                })
                .block());

        assertThat(forwardedRequest.get()).isNotNull();
        assertThat(forwardedRequest.get().headers().getFirst(AuthConstants.HeaderConstants.TENANT_ID))
                .isEqualTo("tenant-1");
        assertThat(TenantExecutionScope.currentTenantId()).isNull();
    }

    @Test
    void tenantForwardingFilterShouldNotInventTenantWithoutContext() {
        AtomicReference<ClientRequest> forwardedRequest = new AtomicReference<>();
        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("http://platform-system/internal/dict/getByType")
        ).build();

        SystemClientConfig.tenantForwardingFilter()
                .filter(request, forwarded -> {
                    forwardedRequest.set(forwarded);
                    return Mono.just(ClientResponse.create(HttpStatus.OK).build());
                })
                .block();

        assertThat(forwardedRequest.get()).isSameAs(request);
        assertThat(forwardedRequest.get().headers().containsHeader(AuthConstants.HeaderConstants.TENANT_ID))
                .isFalse();
    }

    @Test
    void everyClientOperationShouldPreserveOperationAndCauseAsServiceUnavailable() {
        IllegalStateException connectionFailure = new IllegalStateException("connection refused");
        SystemClient client = failingClient(connectionFailure);
        Map<String, Mono<?>> invocations = new LinkedHashMap<>();
        invocations.put("getPushPreferencesForTenant", client.getPushPreferencesForTenant(
                "tenant-1", new PushPreferenceBatchRequest(java.util.List.of("user-1"))));
        invocations.put("getDictByType", client.getDictByType("gender"));
        invocations.put("getImportTemplateFieldByModule", client.getImportTemplateFieldByModule("users"));
        invocations.put("getEnabledImportDefinitions", client.getEnabledImportDefinitions("tenant-1"));
        invocations.put("getTodayWeatherByDept", client.getTodayWeatherByDept("dept-1"));
        invocations.put("getDept", client.getDept("dept-1"));
        invocations.put("getDeptForTenant", client.getDeptForTenant("tenant-1", "dept-1"));
        invocations.put("getUserOrganization", client.getUserOrganization("user-1"));
        invocations.put("getUserOrganizationForTenant",
                client.getUserOrganizationForTenant("tenant-1", "user-1"));
        invocations.put("getUsersByUsernames", client.getUsersByUsernames(
                new com.ai.api.system.dto.UserIdentityBatchRequest(java.util.List.of("user"))));
        invocations.put("isUserInDept", client.isUserInDept("user-1", "dept-1"));
        invocations.put("isUserInDeptForTenant",
                client.isUserInDeptForTenant("tenant-1", "user-1", "dept-1"));

        invocations.forEach((operation, invocation) -> {
            Throwable failure = catchThrowable(invocation::block);

            assertThat(failure)
                    .as(operation)
                    .isInstanceOf(ServiceUnavailableException.class)
                    .hasMessageContaining("platform-system")
                    .hasMessageContaining(operation);
            assertThat(failure.getCause()).as(operation + " cause").isSameAs(connectionFailure);
        });
    }

    private static SystemClient failingClient(Throwable failure) {
        LoadBalancedExchangeFilterFunction loadBalancer = mock(LoadBalancedExchangeFilterFunction.class);
        when(loadBalancer.filter(any(ClientRequest.class), any(ExchangeFunction.class)))
                .thenAnswer(invocation -> {
                    ClientRequest request = invocation.getArgument(0);
                    ExchangeFunction next = invocation.getArgument(1);
                    return next.exchange(request);
                });
        TrustedForwardProperties trustedForwardProperties = new TrustedForwardProperties();
        trustedForwardProperties.setToken("trusted-token");
        WebClient.Builder builder = WebClient.builder()
                .exchangeFunction(request -> Mono.error(failure));

        return new SystemClientConfig().systemClient(loadBalancer, trustedForwardProperties, builder);
    }
}
