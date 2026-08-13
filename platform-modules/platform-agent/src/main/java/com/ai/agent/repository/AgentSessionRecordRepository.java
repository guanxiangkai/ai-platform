package com.ai.agent.repository;

import com.ai.agent.domain.entity.AgentSessionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

/**
 * 通用智能体会话持久化接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface AgentSessionRecordRepository extends JpaRepository<AgentSessionRecord, String>,
        JpaSpecificationExecutor<AgentSessionRecord> {
    Optional<AgentSessionRecord> findByIdAndTenantIdAndDeletedFalse(String id, String tenantId);

    /** 为调用预留锁定会话顺序边界。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from AgentSessionRecord session "
            + "where session.id = :id and session.tenantId = :tenantId and session.deleted = false")
    Optional<AgentSessionRecord> findLockedByIdAndTenantId(
            @Param("id") String id, @Param("tenantId") String tenantId);
}
