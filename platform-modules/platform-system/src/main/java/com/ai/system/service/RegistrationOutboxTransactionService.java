package com.ai.system.service;

import com.ai.api.security.PasswordDigestProtocol;
import com.ai.system.config.RegisterOutboxProperties;
import com.ai.system.domain.OutboxDeliveryState;
import com.ai.system.domain.RegistrationProvisioningPolicy;
import com.ai.system.domain.RegisterState;
import com.ai.system.domain.entity.Register;
import com.ai.system.domain.entity.RegistrationOutbox;
import com.ai.system.domain.entity.UserRole;
import com.ai.system.repository.RegisterRepository;
import com.ai.system.repository.RegistrationOutboxRepository;
import com.ai.system.repository.UserRepository;
import com.ai.system.repository.UserRoleRepository;
import com.ai.system.security.AuthUserCacheService;
import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import com.ai.system.service.impl.RelationEntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * 注册出站任务的短事务状态推进。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class RegistrationOutboxTransactionService {
    private final RegistrationOutboxRepository outboxRepository;
    private final RegisterRepository registerRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantIdProvider tenantIdProvider;
    private final AuthUserCacheService authUserCacheService;
    private final RegisterOutboxProperties properties;

    @Transactional
    public Register claim(String eventId) {
        RegistrationOutbox event = outboxRepository.findLockedById(eventId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (event == null || (event.getDeliveryState() != OutboxDeliveryState.PENDING
                && event.getDeliveryState() != OutboxDeliveryState.RETRY
                && event.getDeliveryState() != OutboxDeliveryState.PROCESSING)
                || event.getAvailableAt() == null
                || event.getAvailableAt().isAfter(now)) {
            return null;
        }
        event.setDeliveryState(OutboxDeliveryState.PROCESSING);
        event.setAvailableAt(now.plus(properties.getProcessingLease()));
        Register record = registerRepository.findLockedById(event.getAggregateId()).orElse(null);
        if (record == null || (record.getRegistrationState() != RegisterState.PROVISIONING
                && record.getRegistrationState() != RegisterState.PROVISIONING_FAILED)) {
            event.setDeliveryState(OutboxDeliveryState.DEAD);
            return null;
        }
        if (!PasswordDigestProtocol.isProtocolBcryptHash(record.getPassword())) {
            event.setDeliveryState(OutboxDeliveryState.DEAD);
            record.setRegistrationState(RegisterState.PROVISIONING_FAILED);
            record.setProvisioningError("注册密码协议无效");
            return null;
        }
        return record;
    }

    @Transactional
    public void activate(String eventId, String roleId) {
        RegistrationOutbox event = outboxRepository.findLockedById(eventId).orElseThrow();
        Register record = registerRepository.findLockedById(event.getAggregateId()).orElseThrow();
        if (event.getDeliveryState() != OutboxDeliveryState.PROCESSING || record.getRegistrationState() == RegisterState.ACTIVE) return;
        UserRole relation = userRoleRepository.findByUserIdAndRoleIdAndDeletedFalse(record.getUserId(), roleId);
        if (relation == null) {
            relation = UserRole.builder().userId(record.getUserId()).roleId(roleId).build();
            userRoleRepository.save(RelationEntityDefaults.ensure(relation, tenantIdProvider));
        }
        userRepository.findById(record.getUserId()).orElseThrow().setEnabled(true);
        record.setRegistrationState(RegisterState.ACTIVE);
        record.setProvisioningError(null);
        event.setDeliveryState(OutboxDeliveryState.SENT);
        authUserCacheService.refreshAfterCommit(record.getUserId(), null, false);
    }

    @Transactional
    public void retry(String eventId, String reason) {
        RegistrationOutbox event = outboxRepository.findLockedById(eventId).orElseThrow();
        Register record = registerRepository.findLockedById(event.getAggregateId()).orElseThrow();
        if (event.getDeliveryState() != OutboxDeliveryState.PROCESSING
                || record.getRegistrationState() == RegisterState.ACTIVE) {
            return;
        }
        event.setAttemptCount(event.getAttemptCount() + 1);
        event.setDeliveryState(event.getAttemptCount() >= properties.getMaxAttempts()
                ? OutboxDeliveryState.DEAD : OutboxDeliveryState.RETRY);
        event.setAvailableAt(LocalDateTime.now().plus(properties.getRetryDelay()));
        event.setLastError(RegistrationProvisioningPolicy.normalizeError(reason));
        record.setRegistrationState(RegisterState.PROVISIONING_FAILED);
        record.setProvisioningError(event.getLastError());
    }
}
