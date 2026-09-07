package com.ai.system.service;

import com.ai.system.domain.RegisterState;
import com.ai.system.domain.entity.Register;
import com.ai.system.repository.RegisterRepository;
import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 注册申请提交事务的租户、重复主体和并发约束测试。 */
class RegistrationSubmissionTransactionServiceTest {

    private final RegisterRepository repository = mock(RegisterRepository.class);
    private final TenantIdProvider tenantIdProvider = mock(TenantIdProvider.class);
    private RegistrationSubmissionTransactionService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationSubmissionTransactionService(repository, tenantIdProvider);
    }

    @Test
    void submitShouldRejectRecordFromAnotherTenant() {
        when(tenantIdProvider.getCurrentTenantId()).thenReturn("tenant-a");
        Register record = record("subject-1");
        record.setTenantId("tenant-b");

        assertThatThrownBy(() -> service.submit(record))
                .isInstanceOf(BizException.class)
                .hasMessage("注册主体不属于当前租户");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void submitShouldCheckOnlyTheCurrentTenantBeforeSaving() {
        when(tenantIdProvider.getCurrentTenantId()).thenReturn("tenant-b");
        Register record = record("subject-shared");
        when(repository.existsByTenantIdAndDirectorySubjectIdAndDeletedFalseAndRegistrationStateNot(
                "tenant-b", "subject-shared", RegisterState.REJECTED)).thenReturn(false);
        when(repository.saveAndFlush(record)).thenReturn(record);

        Register saved = service.submit(record);

        assertThat(saved).isSameAs(record);
        assertThat(record.getTenantId()).isEqualTo("tenant-b");
        assertThat(record.getRegistrationState()).isEqualTo(RegisterState.PENDING);
        verify(repository).existsByTenantIdAndDirectorySubjectIdAndDeletedFalseAndRegistrationStateNot(
                "tenant-b", "subject-shared", RegisterState.REJECTED);
        verify(repository).saveAndFlush(record);
    }

    @Test
    void submitShouldRejectAnExistingActiveDirectorySubjectBeforeSaving() {
        when(tenantIdProvider.getCurrentTenantId()).thenReturn("tenant-a");
        Register record = record("subject-1");
        when(repository.existsByTenantIdAndDirectorySubjectIdAndDeletedFalseAndRegistrationStateNot(
                "tenant-a", "subject-1", RegisterState.REJECTED)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(record))
                .isInstanceOf(BizException.class)
                .hasMessage("该目录主体已有有效注册申请或系统账户");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void submitShouldTranslateTheFlushedUniqueConstraintViolation() {
        when(tenantIdProvider.getCurrentTenantId()).thenReturn("tenant-a");
        Register record = record("subject-1");
        when(repository.existsByTenantIdAndDirectorySubjectIdAndDeletedFalseAndRegistrationStateNot(
                "tenant-a", "subject-1", RegisterState.REJECTED)).thenReturn(false);
        when(repository.saveAndFlush(record)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.submit(record))
                .isInstanceOf(BizException.class)
                .hasMessage("注册信息冲突，请核对后重试");

        verify(repository).saveAndFlush(record);
    }

    private Register record(String directorySubjectId) {
        Register record = new Register();
        record.setDirectorySubjectId(directorySubjectId);
        return record;
    }
}
