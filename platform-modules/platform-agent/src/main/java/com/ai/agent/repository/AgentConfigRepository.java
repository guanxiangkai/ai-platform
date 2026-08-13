package com.ai.agent.repository;

import com.ai.agent.domain.AgentPublishState;
import com.ai.agent.domain.entity.AgentConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 通用智能体定义持久化接口。 */
public interface AgentConfigRepository extends JpaRepository<AgentConfig, String>,
        JpaSpecificationExecutor<AgentConfig> {
    Optional<AgentConfig> findByIdAndTenantIdAndDeletedFalse(String id, String tenantId);

    Optional<AgentConfig> findByTenantIdAndAgentCodeAndDeletedFalse(String tenantId, String agentCode);

    boolean existsByTenantIdAndAgentCodeAndDeletedFalse(String tenantId, String agentCode);

    boolean existsByTenantIdAndAgentCodeAndIdNotAndDeletedFalse(String tenantId, String agentCode, String id);

    List<AgentConfig> findByIdInAndTenantIdAndDeletedFalse(Collection<String> ids, String tenantId);

    Page<AgentConfig> findByTenantIdAndEnabledTrueAndPublishStateAndDeletedFalse(
            String tenantId, AgentPublishState publishState, Pageable pageable);
}
