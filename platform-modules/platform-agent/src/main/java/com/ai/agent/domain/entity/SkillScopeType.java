package com.ai.agent.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** AI 技能作用域类型。 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_skill_scope_type", comment = "AI技能作用域类型表")
public class SkillScopeType extends DataTenantEntity {
    @Column(name = "type_code", nullable = false, length = 32)
    private String typeCode;

    @Column(name = "type_name", nullable = false, length = 64)
    private String typeName;

    @Column(name = "scope_tag", length = 32)
    private String scopeTag;

    @Column(name = "display_in_composer", nullable = false)
    private Boolean displayInComposer = false;

    @Column(name = "default_type", nullable = false)
    private Boolean defaultType = false;

    @Column(name = "weight", nullable = false)
    private Integer weight = 0;
}
