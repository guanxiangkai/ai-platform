package com.ai.system.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.RegisterState;
import com.ai.system.domain.entity.Register;
import com.ai.system.domain.vo.RegisterPageVO;
import com.ai.system.domain.vo.RegisterVO;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 注册记录数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface RegisterRepository extends BaseRepository<RegisterPageVO, RegisterVO, Register> {

    /**
     * 根据用户名判断注册记录是否存在（过滤逻辑删除）
     */
    boolean existsByUsernameAndDeletedFalse(String username);

    /**
     * 根据邮箱判断注册记录是否存在（过滤逻辑删除）
     */
    boolean existsByEmailAndDeletedFalseAndRegistrationStateNot(String email, RegisterState registrationState);

    /**
     * 根据手机号判断注册记录是否存在（过滤逻辑删除）
     */
    boolean existsByPhoneAndDeletedFalseAndRegistrationStateNot(String phone, RegisterState registrationState);

    /**
     * 根据用户名查询注册记录（用于登录时判断账号审核状态）
     */
    Optional<Register> findByUsernameAndDeletedFalse(String username);

    /**
     * 判断租户目录主体是否已有未删除且未被拒绝的注册申请。
     *
     * <p>目录主体是租户外部档案的稳定 ID；租户条件必须显式传入，不能依赖调用方上下文推断。</p>
     */
    boolean existsByTenantIdAndDirectorySubjectIdAndDeletedFalseAndRegistrationStateNot(
            String tenantId, String directorySubjectId, RegisterState registrationState);

    /** 锁定注册记录以执行一次性状态转换。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from Register record where record.id = :id and record.deleted = false")
    Optional<Register> findLockedById(@Param("id") String id);
}
