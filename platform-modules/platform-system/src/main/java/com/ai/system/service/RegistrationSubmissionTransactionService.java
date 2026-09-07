package com.ai.system.service;

import com.ai.system.domain.RegisterState;
import com.ai.system.domain.entity.Register;
import com.ai.system.repository.RegisterRepository;
import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 注册申请本地落库事务边界。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class RegistrationSubmissionTransactionService {

    private final RegisterRepository registerRepository;
    private final TenantIdProvider tenantIdProvider;

    /**
     * 保存已完成目录主体匹配的注册申请。
     *
     * @param record 注册申请
     * @return 已保存的注册申请
     */
    @Transactional
    public Register submit(Register record) {
        String tenantId = tenantIdProvider.getCurrentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException("注册租户信息无效");
        }
        if (StringUtils.hasText(record.getTenantId()) && !tenantId.equals(record.getTenantId())) {
            throw new BizException("注册主体不属于当前租户");
        }
        record.setTenantId(tenantId);
        if (registerRepository.existsByTenantIdAndDirectorySubjectIdAndDeletedFalseAndRegistrationStateNot(
                tenantId, record.getDirectorySubjectId(), RegisterState.REJECTED)) {
            throw new BizException("该目录主体已有有效注册申请或系统账户");
        }
        try {
            record.setRegistrationState(RegisterState.PENDING);
            return registerRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            throw new BizException("注册信息冲突，请核对后重试");
        }
    }
}
