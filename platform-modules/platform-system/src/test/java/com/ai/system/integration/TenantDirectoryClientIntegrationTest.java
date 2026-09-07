package com.ai.system.integration;

import com.ai.system.integration.TenantDirectoryProperties.TenantAdapter;
import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 租户目录 HTTP 适配器的请求与返回契约测试。 */
class TenantDirectoryClientIntegrationTest {

    @Test
    void matchShouldRouteByCurrentTenantEncodeParametersAndForwardTrustedIdentity() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        TenantDirectoryClient client = client("tenant-a", properties(Duration.ofSeconds(1)), request -> {
            captured.set(request);
            return json("{\"subjectId\":\"directory-subject-1\",\"displayName\":\"张三\",\"alreadyLinked\":false}");
        });

        DirectoryMatchResult result = client.matchForRegistration(
                "张 三?/&=", "dept&west?/=+", " 13800138000 ", "a+b@example.com").block();

        assertThat(result).isEqualTo(new DirectoryMatchResult("directory-subject-1", "张三", false));
        assertThat(captured.get().url().getHost()).isEqualTo("directory-a");
        assertThat(captured.get().url().getPath()).isEqualTo("/internal/directory/matchForRegistration");
        assertThat(captured.get().url().getRawQuery())
                .contains("groupId=dept%26west%3F%2F%3D%2B", "email=a%2Bb%40example.com");
        assertThat(captured.get().url().getQuery()).contains("phone=13800138000");
        assertThat(captured.get().headers().getFirst(AuthConstants.HeaderConstants.USER_ID))
                .isEqualTo(AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID);
        assertThat(captured.get().headers().getFirst(AuthConstants.HeaderConstants.TENANT_ID)).isEqualTo("tenant-a");
        assertThat(captured.get().headers().getFirst(new TrustedForwardProperties().getHeaderName()))
                .isEqualTo("trusted-token");
    }

    @Test
    void linkAndAssignmentShouldValidateOpaqueIdsAndEncodeThemInTheirExistingEndpoints() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        TenantDirectoryClient client = client("tenant-a", properties(Duration.ofSeconds(1)), request -> {
            captured.set(request);
            return request.method().name().equals("POST") ? json("true")
                    : json("{\"assignmentId\":\"external-assignment\",\"assignmentName\":\"成员\",\"groupId\":\"external-group\",\"groupName\":\"组织\"}");
        });

        DirectoryAssignment assignment = client.currentAssignment("external/a?b").block();
        assertThat(assignment.assignmentId()).isEqualTo("external-assignment");
        assertThat(assignment.groupId()).isEqualTo("external-group");
        assertThat(captured.get().url().getRawPath()).isEqualTo("/internal/directory/external%2Fa%3Fb/currentAssignment");

        assertThat(client.linkUser("directory/subject?part&x=y", "platform/user?part&x=y").block()).isTrue();
        assertThat(captured.get().url().getPath()).isEqualTo("/internal/directory/linkUser");
        assertThat(captured.get().url().getRawQuery())
                .contains("subjectId=directory%2Fsubject%3Fpart%26x%3Dy",
                        "userId=platform%2Fuser%3Fpart%26x%3Dy");
        assertThatIllegalArgumentException().isThrownBy(() -> client.currentAssignment(" subject"));
        assertThatIllegalArgumentException().isThrownBy(() -> client.linkUser("subject", " "));
    }

    @Test
    void matchResultShouldPreserveValidOpaqueIdButRejectInvalidNonNullValues() {
        assertThat(new DirectoryMatchResult(null, "未匹配", true).alreadyLinked()).isTrue();
        assertThat(new DirectoryMatchResult("opaque-id", "匹配", false).subjectId()).isEqualTo("opaque-id");
        assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryMatchResult("", "", false));
        assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryMatchResult(" opaque-id", "", false));
        assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryMatchResult("x".repeat(65), "", false));
    }

    @Test
    void disabledOrMissingTenantAdapterMustNotFallBackToAnotherTenant() {
        TenantDirectoryProperties properties = new TenantDirectoryProperties(Map.of(
                "tenant-a", new TenantAdapter("directory-a", true),
                "tenant-b", new TenantAdapter("directory-b", false)
        ));
        AtomicInteger requests = new AtomicInteger();
        TenantDirectoryClient disabledClient = client("tenant-b", properties, request -> {
            requests.incrementAndGet();
            return json("{}");
        });
        TenantDirectoryClient missingClient = client("tenant-missing", properties, request -> {
            requests.incrementAndGet();
            return json("{}");
        });

        assertThatThrownBy(() -> disabledClient.matchForRegistration("n", "dept", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未启用目录注册适配器");
        assertThatThrownBy(() -> missingClient.matchForRegistration("n", "dept", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未启用目录注册适配器");
        assertThat(requests.get()).isZero();
    }

    @Test
    void requestTimeoutMustBePositiveAndCancelSlowDirectoryCall() {
        assertThat(new TenantDirectoryProperties(Map.of()).requestTimeout())
                .isEqualTo(TenantDirectoryProperties.DEFAULT_REQUEST_TIMEOUT);
        assertThatIllegalArgumentException().isThrownBy(() -> new TenantDirectoryProperties(Map.of(), Duration.ZERO));
        assertThat(bindProperties(Map.of(
                "platform.directory.tenants.tenant-a.service-name", "directory-a",
                "platform.directory.tenants.tenant-a.registration-enabled", "true"
        )).requestTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(bindProperties(Map.of(
                "platform.directory.tenants.tenant-a.service-name", "directory-a",
                "platform.directory.tenants.tenant-a.registration-enabled", "true",
                "platform.directory.request-timeout", "5s"
        )).requestTimeout()).isEqualTo(Duration.ofSeconds(5));

        AtomicInteger cancellations = new AtomicInteger();
        TenantDirectoryClient client = client("tenant-a", properties(Duration.ofMillis(10)),
                request -> Mono.<ClientResponse>never().doOnCancel(cancellations::incrementAndGet));

        assertThatThrownBy(() -> client.matchForRegistration("n", "dept", null, null).block())
                .hasRootCauseInstanceOf(TimeoutException.class);
        assertThat(cancellations.get()).isEqualTo(1);
    }

    private static TenantDirectoryClient client(String tenantId, TenantDirectoryProperties properties, ExchangeFunction exchange) {
        TenantIdProvider tenantIdProvider = mock(TenantIdProvider.class);
        when(tenantIdProvider.getCurrentTenantId()).thenReturn(tenantId);
        TrustedForwardProperties trustedForward = new TrustedForwardProperties();
        trustedForward.setToken("trusted-token");
        return new TenantDirectoryClient(properties, tenantIdProvider, trustedForward, passthroughLoadBalancer(),
                WebClient.builder().exchangeFunction(exchange));
    }

    private static TenantDirectoryProperties properties(Duration timeout) {
        return new TenantDirectoryProperties(Map.of(
                "tenant-a", new TenantAdapter("directory-a", true),
                "tenant-b", new TenantAdapter("directory-b", true)
        ), timeout);
    }

    private static TenantDirectoryProperties bindProperties(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bind("platform.directory", Bindable.of(TenantDirectoryProperties.class))
                .orElseThrow();
    }

    private static LoadBalancedExchangeFilterFunction passthroughLoadBalancer() {
        LoadBalancedExchangeFilterFunction loadBalancer = mock(LoadBalancedExchangeFilterFunction.class);
        when(loadBalancer.filter(any(ClientRequest.class), any(ExchangeFunction.class)))
                .thenAnswer(invocation -> invocation.<ExchangeFunction>getArgument(1)
                        .exchange(invocation.getArgument(0)));
        return loadBalancer;
    }

    private static Mono<ClientResponse> json(String body) {
        return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }
}
