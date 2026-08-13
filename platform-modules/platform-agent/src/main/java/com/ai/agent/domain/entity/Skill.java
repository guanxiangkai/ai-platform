package com.ai.agent.domain.entity;

import com.ai.agent.domain.SkillActionType;
import com.ai.agent.domain.SkillDisplayMode;
import com.ai.agent.domain.SkillJumpType;
import com.ai.agent.domain.SkillTerminalType;
import com.ai.agent.domain.SkillType;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 跨产品复用的 AI 技能。
 *
 * <p>技能只描述交互动作、作用域和展示方式，不引用任何产品业务实体。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_skill", comment = "AI技能表")
public class Skill extends SortableTenantEntity {
    /** 默认作用域类型编码。 */
    public static final String DEFAULT_SCOPE_TYPE_CODE = "GENERAL";

    /** 默认展示权重。 */
    public static final int DEFAULT_WEIGHT = 50;

    /** 默认排序号。 */
    public static final int DEFAULT_SORT_ORDER = 100;

    @Column(name = "action_prompt", columnDefinition = "text")
    private String actionPrompt;

    @Column(name = "action_title", nullable = false, length = 128)
    private String actionTitle;

    @Column(name = "action_type", nullable = false, length = 32)
    @Convert(converter = SkillActionType.JpaConverter.class)
    private SkillActionType actionType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "jump_type", length = 20)
    @Convert(converter = SkillJumpType.JpaConverter.class)
    private SkillJumpType jumpType;

    @Column(name = "jump_url", length = 500)
    private String jumpUrl;

    @Column(name = "owner_id", length = 64)
    private String ownerId;

    @Column(name = "pinned", nullable = false)
    private Boolean pinned = false;

    @Column(name = "terminal_type", nullable = false, length = 20)
    @Convert(converter = SkillTerminalType.JpaConverter.class)
    private SkillTerminalType terminalType;

    @Column(name = "skill_type", nullable = false, length = 20)
    @Convert(converter = SkillType.JpaConverter.class)
    private SkillType skillType;

    @Column(name = "display_mode", length = 20)
    @Convert(converter = SkillDisplayMode.JpaConverter.class)
    private SkillDisplayMode displayMode;

    @Column(name = "weight", nullable = false)
    private Integer weight = DEFAULT_WEIGHT;

    @Column(name = "scope_type_code", nullable = false, length = 32)
    private String scopeTypeCode = DEFAULT_SCOPE_TYPE_CODE;
}
