package com.ai.files.config;

import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.jpa.plus.starter.JpaPlusHibernateTenantAutoConfiguration;
import io.github.guanxiangkai.jpa.plus.starter.tenant.HibernateTenantContext;
import io.github.guanxiangkai.web.plus.core.context.CurrentUser;
import io.github.guanxiangkai.web.plus.core.context.CurrentUserHolder;
import io.github.guanxiangkai.web.plus.core.spi.CurrentUserProvider;
import io.github.guanxiangkai.web.plus.web.config.WebPlusCoreAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证文件服务装配统一当前用户到 Hibernate 租户保存链。 */
class FilesTenantAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner contextRunner =
            new ReactiveWebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            WebPlusCoreAutoConfiguration.class,
                            JpaPlusHibernateTenantAutoConfiguration.class
                    ))
                    .withPropertyValues("web-plus.cors.enabled=false")
                    .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void exposesCurrentUserTenantToJpaPlusHibernateCustomizer() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CurrentUserProvider.class);
            assertThat(context).hasSingleBean(TenantIdProvider.class);
            assertThat(context).hasBean("jpaPlusHibernateTenantCustomizer");

            CurrentUserHolder.set(currentUser("tenant-files-1"));
            try {
                CurrentUserProvider currentUsers = context.getBean(CurrentUserProvider.class);
                TenantIdProvider tenants = context.getBean(TenantIdProvider.class);
                assertThat(currentUsers.getCurrentUser().orElseThrow().tenantId())
                        .isEqualTo("tenant-files-1");
                assertThat(tenants.getCurrentTenantId()).isEqualTo("tenant-files-1");

                Map<String, Object> hibernateProperties = new HashMap<>();
                context.getBean(
                        "jpaPlusHibernateTenantCustomizer",
                        HibernatePropertiesCustomizer.class
                ).customize(hibernateProperties);
                assertThat(hibernateProperties)
                        .containsKey(HibernateTenantContext.SETTING_KEY);
                assertThat(hibernateProperties.get(HibernateTenantContext.SETTING_KEY))
                        .isInstanceOf(HibernateTenantContext.class);
            } finally {
                CurrentUserHolder.clear();
            }
        });
    }

    private CurrentUser currentUser(String tenantId) {
        return new CurrentUser(
                "internal-service",
                "platform-files",
                tenantId,
                null,
                Set.of(),
                Set.of("INTERNAL_SERVICE"),
                Set.of(),
                false,
                "SERVICE",
                System.currentTimeMillis(),
                Map.of()
        );
    }
}
