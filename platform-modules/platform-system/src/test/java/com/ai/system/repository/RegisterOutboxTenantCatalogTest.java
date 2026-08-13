package com.ai.system.repository;

import com.ai.system.domain.RegistrationOutboxEventType;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注册出站租户目录的最小跨租户查询测试。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
class RegisterOutboxTenantCatalogTest {

    @Test
    void shouldSelectOnlyReadyRegistrationProvisioningTenants() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RegisterOutboxTenantCatalog catalog = new RegisterOutboxTenantCatalog(jdbcTemplate);
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 12, 0);
        when(jdbcTemplate.queryForList(
                contains("event_type = ?"),
                eq(String.class),
                eq(RegistrationOutboxEventType.REGISTER_PROVISION.name()),
                eq(now))).thenReturn(List.of("tenant-a"));

        assertThat(catalog.findReadyTenantIds(now)).containsExactly("tenant-a");

        verify(jdbcTemplate).queryForList(
                contains("delivery_state in ('PENDING', 'RETRY', 'PROCESSING')"),
                eq(String.class),
                eq(RegistrationOutboxEventType.REGISTER_PROVISION.name()),
                eq(now));
    }
}
