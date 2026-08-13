package com.ai.system.service;

import com.ai.system.domain.OutboxDeliveryState;
import com.ai.system.domain.entity.Register;
import com.ai.system.domain.entity.RegistrationOutbox;
import com.ai.system.domain.entity.Role;
import com.ai.system.integration.DirectoryAssignment;
import com.ai.system.integration.TenantDirectoryClient;
import com.ai.system.repository.RegistrationOutboxRepository;
import com.ai.system.repository.RoleRepository;
import com.ai.system.repository.RegisterOutboxTenantCatalog;
import com.ai.system.config.RegisterOutboxProperties;
import com.ai.api.context.TenantExecutionScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 处理已提交的注册开通任务；远程目录服务调用始终在数据库事务外执行。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationOutboxProcessor {
    private final RegistrationOutboxRepository outboxRepository;
    private final RoleRepository roleRepository;
    private final TenantDirectoryClient directoryClient;
    private final RegisterOutboxTenantCatalog tenantCatalog;
    private final RegisterOutboxProperties properties;
    private final RegistrationOutboxTransactionService transactions;

    @Scheduled(fixedDelayString = "${platform.system.register-outbox.interval}")
    public void processReady() {
        LocalDateTime now = LocalDateTime.now();
        tenantCatalog.findReadyTenantIds(now).forEach(tenantId -> processTenant(tenantId, now));
    }

    /** 单个租户读取或处理失败不影响其他租户。 */
    void processTenant(String tenantId, LocalDateTime now) {
        try {
            TenantExecutionScope.run(tenantId, () -> outboxRepository.findReady(
                    List.of(OutboxDeliveryState.PENDING, OutboxDeliveryState.RETRY, OutboxDeliveryState.PROCESSING),
                    now, PageRequest.of(0, properties.getBatchSize())).forEach(this::process));
        } catch (Exception exception) {
            log.error("注册开通任务按租户调度失败: tenantId={}", tenantId, exception);
        }
    }

    /** 单个任务的远程调用不持有数据库事务。 */
    void process(RegistrationOutbox event) {
        Register record = transactions.claim(event.getId());
        if (record == null) {
            return;
        }
        try {
            DirectoryAssignment assignment = directoryClient.currentAssignment(record.getDirectorySubjectId()).block();
            Role role = resolveRole(assignment);
            if (role == null || !Boolean.TRUE.equals(directoryClient.linkUser(
                    record.getDirectorySubjectId(), record.getUserId()).block())) {
                transactions.retry(event.getId(), "目录绑定或角色解析未完成");
                return;
            }
            transactions.activate(event.getId(), role.getId());
        } catch (Exception exception) {
            transactions.retry(event.getId(), exception.getMessage());
        }
    }

    private Role resolveRole(DirectoryAssignment assignment) {
        if (assignment != null && assignment.assignmentName() != null) {
            String combined = (assignment.groupName() == null ? "" : assignment.groupName().trim())
                    + assignment.assignmentName().trim();
            Role matched = roleRepository.findFirstByRoleNameAndEnabledTrueAndDeletedFalse(combined).orElse(null);
            if (matched != null) return matched;
            matched = roleRepository.findFirstByRoleNameAndEnabledTrueAndDeletedFalse(
                    assignment.assignmentName().trim()).orElse(null);
            if (matched != null) return matched;
        }
        return roleRepository.findFirstByDefaultRegistrationRoleTrueAndEnabledTrueAndDeletedFalse().orElse(null);
    }

}
