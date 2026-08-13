package com.ai.agent.repository;

import com.ai.agent.domain.entity.AgentMessageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 通用智能体会话消息持久化接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface AgentMessageRecordRepository extends JpaRepository<AgentMessageRecord, String> {
    List<AgentMessageRecord> findBySessionIdAndTenantIdOrderBySequenceNoAsc(String sessionId, String tenantId);

    Optional<AgentMessageRecord> findByIdAndTenantId(String id, String tenantId);
}
