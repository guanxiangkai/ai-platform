package com.ai.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerEagerLoadProperties;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 网关服务发现预热测试。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
class GatewayServiceDiscoveryWarmupTest {

    @Test
    void shouldResolveEveryConfiguredServiceBeforeStartupCompletes() throws Exception {
        ReactiveDiscoveryClient discoveryClient = mock(ReactiveDiscoveryClient.class);
        LoadBalancerEagerLoadProperties eagerLoad = eagerLoad("platform-auth", "platform-system");
        GatewayDiscoveryWarmupProperties properties = properties();
        when(discoveryClient.getInstances("platform-auth"))
                .thenReturn(Flux.just(instance("platform-auth")));
        when(discoveryClient.getInstances("platform-system"))
                .thenReturn(Flux.just(instance("platform-system")));

        new GatewayServiceDiscoveryWarmup(discoveryClient, eagerLoad, properties)
                .run(new DefaultApplicationArguments(new String[0]));

        verify(discoveryClient).getInstances("platform-auth");
        verify(discoveryClient).getInstances("platform-system");
    }

    @Test
    void shouldRejectServiceWithoutAvailableInstance() {
        ReactiveDiscoveryClient discoveryClient = mock(ReactiveDiscoveryClient.class);
        LoadBalancerEagerLoadProperties eagerLoad = eagerLoad("platform-auth");
        when(discoveryClient.getInstances("platform-auth")).thenReturn(Flux.empty());

        GatewayServiceDiscoveryWarmup warmup = new GatewayServiceDiscoveryWarmup(
                discoveryClient, eagerLoad, properties());

        assertThatThrownBy(() -> warmup.run(new DefaultApplicationArguments(new String[0])))
                .hasMessageContaining("Nacos 中没有可用服务实例: platform-auth");
    }

    private LoadBalancerEagerLoadProperties eagerLoad(String... services) {
        LoadBalancerEagerLoadProperties properties = new LoadBalancerEagerLoadProperties();
        properties.setClients(List.of(services));
        return properties;
    }

    private GatewayDiscoveryWarmupProperties properties() {
        GatewayDiscoveryWarmupProperties properties = new GatewayDiscoveryWarmupProperties();
        properties.setTimeout(Duration.ofSeconds(1));
        properties.setConcurrency(2);
        return properties;
    }

    private DefaultServiceInstance instance(String serviceName) {
        return new DefaultServiceInstance(serviceName + "-1", serviceName, "127.0.0.1", 8080, false);
    }
}
