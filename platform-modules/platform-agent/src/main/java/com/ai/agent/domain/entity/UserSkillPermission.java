package com.ai.agent.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** 用户可在交互端使用的技能白名单。 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_user_skill_permission", comment = "AI用户技能权限表")
public class UserSkillPermission extends SortableTenantEntity {
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "skill_id", nullable = false, length = 64)
    private String skillId;
}
