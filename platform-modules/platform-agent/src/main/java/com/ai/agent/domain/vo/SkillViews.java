package com.ai.agent.domain.vo;

import com.ai.agent.domain.SkillActionType;
import com.ai.agent.domain.SkillDisplayMode;
import com.ai.agent.domain.SkillJumpType;
import com.ai.agent.domain.SkillTerminalType;
import com.ai.agent.domain.SkillType;
import com.ai.agent.domain.entity.Skill;
import com.ai.agent.domain.entity.SkillScopeType;

import java.time.LocalDateTime;

/** 通用技能 API 的稳定视图。 */
public final class SkillViews {
    private SkillViews() {
    }

    /** 技能展示与管理视图。 */
    public record Item(
            String id,
            String desc,
            SkillActionType actionType,
            String actionTitle,
            String actionPrompt,
            SkillType type,
            String scopeTypeCode,
            SkillTerminalType terminalType,
            String jumpUrl,
            SkillJumpType jumpType,
            String ownerId,
            Boolean pinned,
            SkillDisplayMode displayMode,
            Integer sortOrder,
            Integer weight,
            Boolean enabled,
            String remark,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
        /** 从技能实体生成前后端共享视图。 */
        public static Item from(Skill value) {
            return new Item(value.getId(), value.getDescription(), value.getActionType(),
                    value.getActionTitle(), value.getActionPrompt(), value.getSkillType(),
                    value.getScopeTypeCode(), value.getTerminalType(), value.getJumpUrl(),
                    value.getJumpType(), value.getOwnerId(), value.getPinned(), value.getDisplayMode(),
                    value.getSortOrder(), value.getWeight(), value.getEnabled(), value.getRemark(),
                    value.getCreateTime(), value.getUpdateTime());
        }
    }

    /** 可在交互界面使用的技能作用域。 */
    public record ScopeType(
            String id,
            String typeCode,
            String typeName,
            String scopeTag,
            Boolean displayInComposer,
            Boolean defaultType,
            Integer weight,
            Boolean enabled,
            String remark
    ) {
        /** 从作用域实体生成展示视图。 */
        public static ScopeType from(SkillScopeType value) {
            return new ScopeType(value.getId(), value.getTypeCode(), value.getTypeName(),
                    value.getScopeTag(), value.getDisplayInComposer(), value.getDefaultType(),
                    value.getWeight(), value.getEnabled(), value.getRemark());
        }
    }
}
