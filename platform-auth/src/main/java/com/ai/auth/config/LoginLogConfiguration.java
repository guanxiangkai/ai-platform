package com.ai.auth.config;

import io.github.guanxiangkai.web.plus.log.spi.LoginLogHandler;
import com.ai.auth.log.LoginLogHandlerImpl;
import com.ai.auth.properties.AuthSuperAdminProperties;
import com.ai.auth.repository.AuthLoginLogRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Auth 登录日志直接持久化配置。 */
@Configuration(proxyBeanMethods = false)
public class LoginLogConfiguration {

    @Bean
    @Primary
    public LoginLogHandler authLoginLogHandler(
            AuthLoginLogRepository repository,
            AuthSuperAdminProperties superAdminProperties
    ) {
        return new LoginLogHandlerImpl(repository, superAdminProperties);
    }
}
