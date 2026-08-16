package com.ai.api.agent.config;

import com.ai.api.agent.client.AgentClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.reactive.ReactiveHttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.boot.webclient.autoconfigure.service.ReactiveHttpServiceClientAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerWebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgentClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    HttpServiceClientPropertiesAutoConfiguration.class,
                    ReactiveHttpClientAutoConfiguration.class,
                    WebClientAutoConfiguration.class,
                    AgentClientConfig.class,
                    ReactiveHttpServiceClientAutoConfiguration.class))
            .withPropertyValues("web-plus.security.trusted-forward.token=trusted-token");

    @Test
    void shouldBackOffWhenLoadBalancerIsUnavailable() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AgentClient.class);
            assertThat(context).doesNotHaveBean(HttpServiceProxyRegistry.class);
        });
    }

    @Test
    void shouldRegisterHttpServiceProxyAndFacade() {
        contextRunner
                .withBean(LoadBalancerWebClientHttpServiceGroupConfigurer.class,
                        () -> mock(LoadBalancerWebClientHttpServiceGroupConfigurer.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpServiceProxyRegistry.class);
                    assertThat(context).hasSingleBean(AgentClientConfig.InternalAgentClient.class);
                    assertThat(context).hasSingleBean(AgentClient.class);
                });
    }

    @Test
    void shouldBackOffWhenCallerProvidesClient() {
        AgentClient customClient = (agentCode, idempotencyKey, request) ->
                reactor.core.publisher.Mono.empty();
        contextRunner
                .withBean(LoadBalancerWebClientHttpServiceGroupConfigurer.class,
                        () -> mock(LoadBalancerWebClientHttpServiceGroupConfigurer.class))
                .withBean(AgentClient.class, () -> customClient)
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentClient.class);
                    assertThat(context.getBean(AgentClient.class)).isSameAs(customClient);
                    assertThat(context).doesNotHaveBean(HttpServiceProxyRegistry.class);
                });
    }
}
