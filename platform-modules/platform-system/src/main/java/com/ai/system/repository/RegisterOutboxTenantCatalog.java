package com.ai.system.repository;

import com.ai.system.domain.RegistrationOutboxEventType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 从共享出站表中列出存在待开通注册任务的租户。
 *
 * <p>该查询是后台调度唯一允许的跨租户目录边界，只返回租户标识；
 * 后续实体读写必须进入 {@code TenantExecutionScope} 并继续由 JPA Plus 强制隔离。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public class RegisterOutboxTenantCatalog {

    private final JdbcTemplate jdbcTemplate;

    public RegisterOutboxTenantCatalog(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询当前时间已到期的注册开通任务所属租户。
     *
     * @param now 调度时间
     * @return 去重的租户标识
     */
    public List<String> findReadyTenantIds(LocalDateTime now) {
        return jdbcTemplate.queryForList("""
                        select distinct tenant_id
                        from sys_outbox
                        where deleted = false
                          and event_type = ?
                          and delivery_state in ('PENDING', 'RETRY', 'PROCESSING')
                          and available_at <= ?
                        """,
                String.class,
                RegistrationOutboxEventType.REGISTER_PROVISION.name(),
                now);
    }
}
