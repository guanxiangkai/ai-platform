package com.ai.system.service;

import com.ai.system.config.RegisterOutboxProperties;
import com.ai.system.domain.OutboxDeliveryState;
import com.ai.system.domain.RegisterState;
import com.ai.system.domain.RegistrationProvisioningPolicy;
import com.ai.system.domain.entity.Register;
import com.ai.system.domain.entity.RegistrationOutbox;
import com.ai.system.domain.entity.User;
import com.ai.system.domain.entity.UserRole;
import com.ai.system.repository.RegisterRepository;
import com.ai.system.repository.RegistrationOutboxRepository;
import com.ai.system.repository.UserRepository;
import com.ai.system.repository.UserRoleRepository;
import com.ai.system.security.AuthUserCacheService;
import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 注册开通短事务的并发与幂等行为测试。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
class RegistrationOutboxTransactionServiceTest {

    private final RegistrationOutboxRepository outboxRepository = mock(RegistrationOutboxRepository.class);
    private final RegisterRepository registerRepository = mock(RegisterRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
    private final TenantIdProvider tenantIdProvider = mock(TenantIdProvider.class);
    private final AuthUserCacheService authUserCacheService = mock(AuthUserCacheService.class);
    private final RegisterOutboxProperties properties = new RegisterOutboxProperties();

    private RegistrationOutboxTransactionService service;

    @BeforeEach
    void setUp() {
        properties.setProcessingLease(Duration.ofMinutes(2));
        properties.setRetryDelay(Duration.ofMinutes(1));
        properties.setMaxAttempts(3);
        service = new RegistrationOutboxTransactionService(
                outboxRepository,
                registerRepository,
                userRepository,
                userRoleRepository,
                tenantIdProvider,
                authUserCacheService,
                properties);
    }

    @Test
    void claimShouldRejectAnUnexpiredProcessingLease() {
        RegistrationOutbox event = event(OutboxDeliveryState.PROCESSING);
        event.setAvailableAt(LocalDateTime.now().plusMinutes(1));
        when(outboxRepository.findLockedById(event.getId())).thenReturn(Optional.of(event));

        assertThat(service.claim(event.getId())).isNull();

        assertThat(event.getDeliveryState()).isEqualTo(OutboxDeliveryState.PROCESSING);
        verifyNoInteractions(registerRepository);
    }

    @Test
    void claimShouldLeaseAReadyEventAndReturnItsProvisioningRecord() {
        RegistrationOutbox event = event(OutboxDeliveryState.RETRY);
        event.setAvailableAt(LocalDateTime.now().minusSeconds(1));
        Register record = record(RegisterState.PROVISIONING_FAILED);
        when(outboxRepository.findLockedById(event.getId())).thenReturn(Optional.of(event));
        when(registerRepository.findLockedById(event.getAggregateId())).thenReturn(Optional.of(record));

        LocalDateTime beforeClaim = LocalDateTime.now();
        assertThat(service.claim(event.getId())).isSameAs(record);

        assertThat(event.getDeliveryState()).isEqualTo(OutboxDeliveryState.PROCESSING);
        assertThat(event.getAvailableAt()).isAfterOrEqualTo(beforeClaim.plus(properties.getProcessingLease()));
    }

    @Test
    void claimShouldRejectAProvisioningRecordWithCustomMarker() {
        RegistrationOutbox event = event(OutboxDeliveryState.PENDING);
        Register record = record(RegisterState.PROVISIONING);
        record.setPassword("{sha1-bcrypt}$2b$12$" + "A".repeat(53));
        when(outboxRepository.findLockedById(event.getId())).thenReturn(Optional.of(event));
        when(registerRepository.findLockedById(event.getAggregateId())).thenReturn(Optional.of(record));

        assertThat(service.claim(event.getId())).isNull();

        assertThat(event.getDeliveryState()).isEqualTo(OutboxDeliveryState.DEAD);
        assertThat(record.getRegistrationState()).isEqualTo(RegisterState.PROVISIONING_FAILED);
        assertThat(record.getProvisioningError()).isEqualTo("注册密码协议无效");
    }

    @Test
    void claimShouldRejectAProvisioningRecordWithUnsupportedBcryptCost() {
        RegistrationOutbox event = event(OutboxDeliveryState.PENDING);
        Register record = record(RegisterState.PROVISIONING);
        record.setPassword("$2b$32$" + "A".repeat(53));
        when(outboxRepository.findLockedById(event.getId())).thenReturn(Optional.of(event));
        when(registerRepository.findLockedById(event.getAggregateId())).thenReturn(Optional.of(record));

        assertThat(service.claim(event.getId())).isNull();

        assertThat(event.getDeliveryState()).isEqualTo(OutboxDeliveryState.DEAD);
        assertThat(record.getRegistrationState()).isEqualTo(RegisterState.PROVISIONING_FAILED);
        assertThat(record.getProvisioningError()).isEqualTo("注册密码协议无效");
    }

    @Test
    void activateShouldAssignRoleOnceEnableUserAndFinalizeTheState() {
        RegistrationOutbox event = event(OutboxDeliveryState.PROCESSING);
        Register record = record(RegisterState.PROVISIONING);
        User user = new User();
        user.setEnabled(false);
        user.setId(record.getUserId());
        when(outboxRepository.findLockedById(event.getId())).thenReturn(Optional.of(event));
        when(registerRepository.findLockedById(event.getAggregateId())).thenReturn(Optional.of(record));
        when(userRoleRepository.findByUserIdAndRoleIdAndDeletedFalse(record.getUserId(), "role-1"))
                .thenReturn(null);
        when(userRepository.findById(record.getUserId())).thenReturn(Optional.of(user));
        when(tenantIdProvider.getCurrentTenantId()).thenReturn("tenant-a");

        service.activate(event.getId(), "role-1");

        assertThat(user.getEnabled()).isTrue();
        assertThat(record.getRegistrationState()).isEqualTo(RegisterState.ACTIVE);
        assertThat(event.getDeliveryState()).isEqualTo(OutboxDeliveryState.SENT);
        verify(userRoleRepository).save(any(UserRole.class));
        verify(authUserCacheService).refreshAfterCommit(record.getUserId(), null, false);
    }

    @Test
    void activateShouldReuseAnExistingRoleRelation() {
        RegistrationOutbox event = event(OutboxDeliveryState.PROCESSING);
        Register record = record(RegisterState.PROVISIONING);
        User user = new User();
        user.setEnabled(false);
        user.setId(record.getUserId());
        UserRole existing = UserRole.builder().userId(record.getUserId()).roleId("role-1").build();
        when(outboxRepository.findLockedById(event.getId())).thenReturn(Optional.of(event));
        when(registerRepository.findLockedById(event.getAggregateId())).thenReturn(Optional.of(record));
        when(userRoleRepository.findByUserIdAndRoleIdAndDeletedFalse(record.getUserId(), "role-1"))
                .thenReturn(existing);
        when(userRepository.findById(record.getUserId())).thenReturn(Optional.of(user));

        service.activate(event.getId(), "role-1");

        verify(userRoleRepository, never()).save(any(UserRole.class));
        assertThat(record.getRegistrationState()).isEqualTo(RegisterState.ACTIVE);
    }

    @Test
    void retryShouldDeadLetterAtTheAttemptLimitAndBoundTheErrorText() {
        RegistrationOutbox event = event(OutboxDeliveryState.PROCESSING);
        event.setAttemptCount(properties.getMaxAttempts() - 1);
        Register record = record(RegisterState.PROVISIONING);
        when(outboxRepository.findLockedById(event.getId())).thenReturn(Optional.of(event));
        when(registerRepository.findLockedById(event.getAggregateId())).thenReturn(Optional.of(record));

        service.retry(event.getId(), "x".repeat(RegistrationProvisioningPolicy.ERROR_MAX_LENGTH + 10));

        assertThat(event.getAttemptCount()).isEqualTo(properties.getMaxAttempts());
        assertThat(event.getDeliveryState()).isEqualTo(OutboxDeliveryState.DEAD);
        assertThat(event.getLastError()).hasSize(RegistrationProvisioningPolicy.ERROR_MAX_LENGTH);
        assertThat(record.getRegistrationState()).isEqualTo(RegisterState.PROVISIONING_FAILED);
        assertThat(record.getProvisioningError()).isEqualTo(event.getLastError());
    }

    @Test
    void retryShouldNotReopenAnAlreadyActivatedRegistration() {
        RegistrationOutbox event = event(OutboxDeliveryState.SENT);
        event.setAttemptCount(1);
        Register record = record(RegisterState.ACTIVE);
        when(outboxRepository.findLockedById(event.getId())).thenReturn(Optional.of(event));
        when(registerRepository.findLockedById(event.getAggregateId())).thenReturn(Optional.of(record));

        service.retry(event.getId(), "过期失败回调");

        assertThat(event.getDeliveryState()).isEqualTo(OutboxDeliveryState.SENT);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(record.getRegistrationState()).isEqualTo(RegisterState.ACTIVE);
    }

    private RegistrationOutbox event(OutboxDeliveryState state) {
        RegistrationOutbox event = new RegistrationOutbox();
        event.setId("event-1");
        event.setAggregateId("register-1");
        event.setDeliveryState(state);
        event.setAttemptCount(0);
        event.setAvailableAt(LocalDateTime.now());
        return event;
    }

    private Register record(RegisterState state) {
        Register record = new Register();
        record.setId("register-1");
        record.setDirectorySubjectId("subject-1");
        record.setUserId("user-1");
        record.setPassword("$2b$12$" + "A".repeat(53));
        record.setRegistrationState(state);
        return record;
    }
}
