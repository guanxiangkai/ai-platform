package com.ai.system.service;

import com.ai.api.context.TenantExecutionScope;
import com.ai.system.config.RegisterOutboxProperties;
import com.ai.system.domain.OutboxDeliveryState;
import com.ai.system.domain.RegisterState;
import com.ai.system.domain.entity.Register;
import com.ai.system.domain.entity.RegistrationOutbox;
import com.ai.system.domain.entity.Role;
import com.ai.system.integration.TenantWorkforceClient;
import com.ai.system.integration.WorkforcePosition;
import com.ai.system.repository.RegisterOutboxTenantCatalog;
import com.ai.system.repository.RegistrationOutboxRepository;
import com.ai.system.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注册开通调度的租户隔离和远程编排测试。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
class RegistrationOutboxProcessorTest {

    private final RegistrationOutboxRepository outboxRepository = mock(RegistrationOutboxRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final TenantWorkforceClient workforceClient = mock(TenantWorkforceClient.class);
    private final RegisterOutboxTenantCatalog tenantCatalog = mock(RegisterOutboxTenantCatalog.class);
    private final RegisterOutboxProperties properties = new RegisterOutboxProperties();
    private final RegistrationOutboxTransactionService transactions =
            mock(RegistrationOutboxTransactionService.class);

    private RegistrationOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new RegistrationOutboxProcessor(
                outboxRepository,
                roleRepository,
                workforceClient,
                tenantCatalog,
                properties,
                transactions);
    }

    @Test
    void processShouldActivateAfterPositionRoleAndBindingSucceed() {
        RegistrationOutbox event = event();
        Register record = record();
        WorkforcePosition position = new WorkforcePosition("position-1", "操作员", "dept-1", "运行部");
        Role role = new Role();
        role.setId("role-1");
        when(transactions.claim(event.getId())).thenReturn(record);
        when(workforceClient.currentPosition(record.getPersonnelId())).thenReturn(Mono.just(position));
        when(roleRepository.findFirstByRoleNameAndEnabledTrueAndDeletedFalse("运行部操作员"))
                .thenReturn(Optional.of(role));
        when(workforceClient.bindUser(record.getPersonnelId(), record.getUserId())).thenReturn(Mono.just(true));

        processor.process(event);

        verify(transactions).activate(event.getId(), role.getId());
        verify(transactions, never()).retry(any(), any());
    }

    @Test
    void processShouldRetryWhenBindingReturnsFalse() {
        RegistrationOutbox event = event();
        Register record = record();
        Role role = new Role();
        role.setId("role-1");
        when(transactions.claim(event.getId())).thenReturn(record);
        when(workforceClient.currentPosition(record.getPersonnelId())).thenReturn(Mono.empty());
        when(roleRepository.findFirstByDefaultRegistrationRoleTrueAndEnabledTrueAndDeletedFalse())
                .thenReturn(Optional.of(role));
        when(workforceClient.bindUser(record.getPersonnelId(), record.getUserId())).thenReturn(Mono.just(false));

        processor.process(event);

        verify(transactions).retry(event.getId(), "人员绑定或角色解析未完成");
        verify(transactions, never()).activate(any(), any());
    }

    @Test
    void processShouldNotPersistRemoteExceptionMessage() {
        RegistrationOutbox event = event();
        Register record = record();
        when(transactions.claim(event.getId())).thenReturn(record);
        when(workforceClient.currentPosition(record.getPersonnelId())).thenReturn(Mono.error(
                new IllegalStateException("remote-endpoint-detail")));

        processor.process(event);

        verify(transactions).retry(event.getId(), "人员开通依赖调用失败");
        verify(transactions, never()).activate(any(), any());
    }

    @Test
    void processReadyShouldIsolateTenantFailuresAndRestoreTheScope() {
        when(tenantCatalog.findReadyTenantIds(any(LocalDateTime.class)))
                .thenReturn(List.of("tenant-a", "tenant-b"));
        List<String> observedScopes = new ArrayList<>();
        when(outboxRepository.findReady(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    String tenantId = TenantExecutionScope.currentTenantId();
                    observedScopes.add(tenantId);
                    if ("tenant-a".equals(tenantId)) {
                        throw new IllegalStateException("租户 A 数据库暂时不可用");
                    }
                    return List.of();
                });

        assertThatCode(processor::processReady).doesNotThrowAnyException();

        assertThat(observedScopes).containsExactly("tenant-a", "tenant-b");
        assertThat(TenantExecutionScope.currentTenantId()).isNull();
    }

    private RegistrationOutbox event() {
        RegistrationOutbox event = new RegistrationOutbox();
        event.setId("event-1");
        event.setDeliveryState(OutboxDeliveryState.PENDING);
        return event;
    }

    private Register record() {
        Register record = new Register();
        record.setId("register-1");
        record.setPersonnelId("personnel-1");
        record.setUserId("user-1");
        record.setRegistrationState(RegisterState.PROVISIONING);
        return record;
    }
}
