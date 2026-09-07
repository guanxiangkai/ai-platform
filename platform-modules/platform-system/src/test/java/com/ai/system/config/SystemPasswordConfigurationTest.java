package com.ai.system.config;

import io.github.guanxiangkai.web.plus.security.password.ProtocolPasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPasswordConfigurationTest {

    @Test
    void protocolEncoderIsPrimaryOverDefaultPasswordEncoder() {
        new ApplicationContextRunner()
                .withBean("defaultPasswordEncoder", PasswordEncoder.class, BCryptPasswordEncoder::new)
                .withUserConfiguration(SystemPasswordConfiguration.class)
                .run(context -> assertThat(context.getBean(PasswordEncoder.class))
                        .isInstanceOf(ProtocolPasswordEncoder.class));
    }
}
