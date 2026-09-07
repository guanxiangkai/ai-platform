package com.ai.system.service.impl;

import com.ai.system.domain.RegisterState;
import com.ai.system.domain.dto.RegisterCreateDTO;
import com.ai.system.domain.entity.Dept;
import com.ai.system.domain.entity.Register;
import com.ai.system.integration.DirectoryMatchResult;
import com.ai.system.integration.TenantDirectoryClient;
import com.ai.system.repository.DeptRepository;
import com.ai.system.repository.RegisterRepository;
import com.ai.system.repository.RegistrationOutboxRepository;
import com.ai.system.repository.UserRepository;
import com.ai.system.service.RegistrationSubmissionTransactionService;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 注册申请联系方式重复规则测试。 */
class RegisterServiceImplTest {

    private final RegisterRepository registerRepository = mock(RegisterRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DeptRepository deptRepository = mock(DeptRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final TenantDirectoryClient directoryClient = mock(TenantDirectoryClient.class);
    private final RegistrationOutboxRepository outboxRepository = mock(RegistrationOutboxRepository.class);
    private final RegistrationSubmissionTransactionService submissionService =
            mock(RegistrationSubmissionTransactionService.class);
    private RegisterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RegisterServiceImpl(registerRepository, userRepository, deptRepository, passwordEncoder,
                directoryClient, outboxRepository, submissionService);
    }

    @Test
    void registerShouldAllowRejectedContactApplicationsToBeResubmitted() {
        Dept dept = new Dept();
        dept.setDeptCode("dev");
        when(deptRepository.findByIdAndDeletedFalse("dept-1")).thenReturn(Optional.of(dept));
        when(directoryClient.matchForRegistration("张三", "dept-1", "13800138000", "zhangsan@example.com"))
                .thenReturn(Mono.just(new DirectoryMatchResult("subject-1", "张三", false)));
        when(passwordEncoder.encode("a".repeat(40))).thenReturn("encoded-password");
        when(submissionService.submit(any(Register.class))).thenAnswer(invocation -> {
            Register record = invocation.getArgument(0);
            record.setId("register-1");
            return record;
        });

        service.register(dto());

        verify(registerRepository).existsByEmailAndDeletedFalseAndRegistrationStateNot(
                "zhangsan@example.com", RegisterState.REJECTED);
        verify(registerRepository).existsByPhoneAndDeletedFalseAndRegistrationStateNot(
                "13800138000", RegisterState.REJECTED);
    }

    @Test
    void registerShouldStillRejectAnEmailAlreadyUsedByAnAccount() {
        Dept dept = new Dept();
        when(deptRepository.findByIdAndDeletedFalse("dept-1")).thenReturn(Optional.of(dept));
        when(userRepository.existsByEmailAndDeletedFalse("zhangsan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(dto()))
                .isInstanceOf(BizException.class)
                .hasMessage("该邮箱已被使用");
    }

    private RegisterCreateDTO dto() {
        return new RegisterCreateDTO("张三", "a".repeat(40), "dept-1", "张三",
                "zhangsan@example.com", "13800138000", "1");
    }
}
