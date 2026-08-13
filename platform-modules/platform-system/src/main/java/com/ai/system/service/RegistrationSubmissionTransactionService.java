package com.ai.system.service;

import com.ai.system.domain.RegisterState;
import com.ai.system.domain.entity.Register;
import com.ai.system.repository.RegisterRepository;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 保存已完成外部人员匹配的注册申请。
     *
     * @param record 注册申请
     * @return 已保存的注册申请
     */
    @Transactional
    public Register submit(Register record) {
        try {
            record.setRegistrationState(RegisterState.PENDING);
            return registerRepository.save(record);
        } catch (DataIntegrityViolationException exception) {
            throw new BizException("该员工档案已有有效注册申请或系统账户");
        }
    }
}
