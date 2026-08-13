package com.ai.agent.repository;

import com.ai.agent.domain.entity.SkillScopeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 技能作用域类型持久化接口。 */
public interface SkillScopeTypeRepository extends JpaRepository<SkillScopeType, String> {
    Page<SkillScopeType> findByTenantIdAndDeletedFalseAndEnabledTrueAndDisplayInComposerTrue(
            String tenantId, Pageable pageable);

    boolean existsByTenantIdAndTypeCodeAndDeletedFalseAndEnabledTrue(String tenantId, String typeCode);
}
