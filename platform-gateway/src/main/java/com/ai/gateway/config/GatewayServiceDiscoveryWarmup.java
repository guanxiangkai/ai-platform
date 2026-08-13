package com.ai.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerEagerLoadProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * 在网关完成启动前主动建立全部下游服务的 Nacos 订阅。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayServiceDiscoveryWarmup implements ApplicationRunner {

    private final ReactiveDiscoveryClient discoveryClient;
    private final LoadBalancerEagerLoadProperties eagerLoadProperties;
    private final GatewayDiscoveryWarmupProperties properties;

    @Override
    public void run(ApplicationArguments arguments) {
        List<String> services = normalizedServices();
        if (services.isEmpty()) {
            throw new IllegalStateException("网关服务发现预热列表不能为空");
        }

        List<ServiceWarmupResult> results = Flux.fromIterable(services)
                .flatMapSequential(this::warmService, properties.getConcurrency())
                .collectList()
                .block();
        if (results == null || results.size() != services.size()) {
            throw new IllegalStateException("网关服务发现预热结果不完整");
        }
        log.info("网关服务发现预热完成: serviceCount={}", results.size());
    }

    private Mono<ServiceWarmupResult> warmService(String serviceName) {
        return discoveryClient.getInstances(serviceName)
                .collectList()
                .timeout(properties.getTimeout())
                .map(instances -> {
                    if (instances.isEmpty()) {
                        throw new IllegalStateException("Nacos 中没有可用服务实例: " + serviceName);
                    }
                    return new ServiceWarmupResult(serviceName, instances.size());
                })
                .doOnNext(result -> log.info(
                        "网关服务发现预热成功: serviceName={}, instanceCount={}",
                        result.serviceName(), result.instanceCount()));
    }

    private List<String> normalizedServices() {
        List<String> configured = eagerLoadProperties.getClients();
        if (configured == null) {
            return List.of();
        }
        return configured.stream()
                .filter(service -> service != null && !service.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));
    }

    private record ServiceWarmupResult(String serviceName, int instanceCount) {
    }
}
