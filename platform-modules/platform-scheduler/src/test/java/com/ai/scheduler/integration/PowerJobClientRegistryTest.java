package com.ai.scheduler.integration;

import com.ai.scheduler.config.SchedulerProperties;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PowerJobClientRegistryTest {

    @Test
    void rejectsMissingApplicationConfiguration() {
        PowerJobClientRegistry registry = new PowerJobClientRegistry(new SchedulerProperties());

        assertThatThrownBy(() -> registry.client("example", null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("调度应用配置不存在");
    }

    @Test
    void rejectsMissingServerAddressBeforeCreatingClient() {
        SchedulerProperties properties = new SchedulerProperties();
        properties.setServerAddresses(List.of(" "));
        SchedulerProperties.Application application = application("example-worker", "secret");
        PowerJobClientRegistry registry = new PowerJobClientRegistry(properties);

        assertThatThrownBy(() -> registry.client("example", application))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("PowerJob Server 地址未配置");
    }

    @Test
    void rejectsMissingApplicationCredentialBeforeCreatingClient() {
        SchedulerProperties properties = new SchedulerProperties();
        properties.setServerAddresses(List.of("powerjob-server:7700"));
        PowerJobClientRegistry registry = new PowerJobClientRegistry(properties);

        assertThatThrownBy(() -> registry.client("example", application("example-worker", " ")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("PowerJob 应用名称或访问凭据未配置");
    }

    private SchedulerProperties.Application application(String appName, String password) {
        SchedulerProperties.Application application = new SchedulerProperties.Application();
        application.setAppName(appName);
        application.setPassword(password);
        return application;
    }
}
