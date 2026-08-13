package com.ai.api.system.config;

import com.ai.api.system.client.SystemClient;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SystemClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SystemClientConfig.class))
            .withBean(TrustedForwardProperties.class, () -> trustedForwardProperties("trusted-token"))
            .withBean(LoadBalancedExchangeFilterFunction.class,
                    () -> mock(LoadBalancedExchangeFilterFunction.class));

    @Test
    void shouldBackOffWhenWebClientBuilderIsUnavailable() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(SystemClient.class));
    }

    @Test
    void shouldCreateClientWhenRequiredClientInfrastructureIsAvailable() {
        contextRunner
                .withBean(WebClient.Builder.class, WebClient::builder)
                .run(context -> assertThat(context).hasSingleBean(SystemClient.class));
    }

    private static TrustedForwardProperties trustedForwardProperties(String token) {
        TrustedForwardProperties properties = new TrustedForwardProperties();
        properties.setToken(token);
        return properties;
    }
}
