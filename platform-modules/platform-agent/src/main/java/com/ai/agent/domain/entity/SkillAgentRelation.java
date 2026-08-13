package com.ai.agent.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 当前租户内技能与可调用智能体的一对一关系。
 *
 * <p>每个技能至多绑定一个已发布且启用的智能体，由数据库唯一约束和本实体共同保证。</p>
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_skill_agent_relation", comment = "AI技能与智能体关系表")
public class SkillAgentRelation extends SortableTenantEntity {
    @Column(name = "skill_id", nullable = false, length = 64)
    private String skillId;

    @Column(name = "agent_id", nullable = false, length = 64)
    private String agentId;
}
