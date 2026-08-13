package com.ai.agent.repository;

import com.ai.agent.domain.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 通用技能持久化接口。 */
public interface SkillRepository extends JpaRepository<Skill, String>, JpaSpecificationExecutor<Skill> {
    Optional<Skill> findByIdAndTenantIdAndDeletedFalse(String id, String tenantId);

    List<Skill> findByIdInAndTenantIdAndDeletedFalse(Collection<String> ids, String tenantId);
}
