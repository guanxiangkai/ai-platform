package com.ai.agent.domain.vo;

import com.ai.agent.domain.entity.AgentConfig;
import com.ai.agent.domain.entity.Skill;

/** 技能授权管理 API 的稳定视图。 */
public final class SkillAuthorizationViews {
    private SkillAuthorizationViews() {
    }

    /** 技能及其当前绑定智能体。 */
    public record Relation(
            String skillId,
            String skillName,
            String scopeTypeCode,
            String agentId,
            String agentName,
            String provider) {
        /** 由租户内技能及其可选绑定智能体生成视图。 */
        public static Relation from(Skill skill, AgentConfig agent) {
            return new Relation(skill.getId(), skill.getActionTitle(), skill.getScopeTypeCode(),
                    agent == null ? null : agent.getId(),
                    agent == null ? null : agent.getAgentName(),
                    agent == null || agent.getProviderType() == null ? null : agent.getProviderType().name());
        }
    }
}
