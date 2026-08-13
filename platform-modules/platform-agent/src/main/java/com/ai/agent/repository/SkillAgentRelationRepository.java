package com.ai.agent.repository;

import com.ai.agent.domain.entity.SkillAgentRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 技能与智能体关系的租户隔离持久化接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface SkillAgentRelationRepository extends JpaRepository<SkillAgentRelation, String> {
    List<SkillAgentRelation> findByTenantIdAndSkillIdInAndDeletedFalse(String tenantId, List<String> skillIds);

    Optional<SkillAgentRelation> findByTenantIdAndSkillIdAndDeletedFalse(String tenantId, String skillId);

    /** 查询当前租户内已启用的技能智能体绑定。 */
    Optional<SkillAgentRelation> findByTenantIdAndSkillIdAndEnabledTrueAndDeletedFalse(
            String tenantId, String skillId);
}
