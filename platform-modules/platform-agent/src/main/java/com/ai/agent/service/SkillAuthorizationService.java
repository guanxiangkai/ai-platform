package com.ai.agent.service;

import com.ai.agent.domain.AgentPublishState;
import com.ai.agent.config.AgentInvocationProperties;
import com.ai.agent.domain.dto.SkillAuthorizationRequest;
import com.ai.agent.domain.entity.AgentConfig;
import com.ai.agent.domain.entity.Skill;
import com.ai.agent.domain.entity.SkillAgentRelation;
import com.ai.agent.domain.entity.UserSkillPermission;
import com.ai.agent.domain.vo.SkillAuthorizationViews;
import com.ai.agent.repository.AgentConfigRepository;
import com.ai.agent.repository.SkillAgentRelationRepository;
import com.ai.agent.repository.SkillRepository;
import com.ai.agent.repository.UserSkillPermissionRepository;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 通用技能与智能体、用户白名单关系的应用服务。 */
@Service
@RequiredArgsConstructor
public class SkillAuthorizationService {
    private final AgentConfigRepository agents;
    private final SkillRepository skills;
    private final SkillAgentRelationRepository relations;
    private final UserSkillPermissionRepository userPermissions;
    private final AgentInvocationProperties invocationProperties;

    /** 查询可用于技能绑定的已发布智能体下拉项。 */
    public List<OptionItem> agentOptions() {
        return agents.findByTenantIdAndEnabledTrueAndPublishStateAndDeletedFalse(
                        currentTenantId(), AgentPublishState.PUBLISHED,
                        PageRequest.of(0, invocationProperties.getSkillSelectionLimit(), Sort.by("agentName").ascending()))
                .getContent()
                .stream()
                .map(agent -> OptionItem.of(agent.getAgentName(), agent.getId()))
                .toList();
    }

    /** 查询当前租户所有技能及各自绑定的智能体。 */
    public List<SkillAuthorizationViews.Relation> skillRelations() {
        String tenantId = currentTenantId();
        List<Skill> tenantSkills = skills.findAll(tenantSpecification(tenantId), PageRequest.of(
                        0, invocationProperties.getSkillSelectionLimit(),
                        Sort.by(Sort.Order.asc("sortInfo.sortOrder"), Sort.Order.asc("actionTitle"))))
                .getContent();
        List<String> skillIds = tenantSkills.stream().map(Skill::getId).toList();
        Map<String, SkillAgentRelation> relationBySkillId = (skillIds.isEmpty() ? List.<SkillAgentRelation>of()
                : relations.findByTenantIdAndSkillIdInAndDeletedFalse(tenantId, skillIds))
                .stream()
                .collect(Collectors.toMap(SkillAgentRelation::getSkillId, Function.identity(), (left, right) -> left));
        List<String> agentIds = relationBySkillId.values().stream().map(SkillAgentRelation::getAgentId).toList();
        Map<String, AgentConfig> agentById = agentIds.isEmpty() ? Map.of()
                : agents.findByIdInAndTenantIdAndDeletedFalse(agentIds, tenantId)
                .stream()
                .collect(Collectors.toMap(AgentConfig::getId, Function.identity()));
        return tenantSkills.stream()
                .map(skill -> {
                    SkillAgentRelation relation = relationBySkillId.get(skill.getId());
                    AgentConfig agent = relation == null ? null : agentById.get(relation.getAgentId());
                    return SkillAuthorizationViews.Relation.from(skill, agent);
                })
                .toList();
    }

    /** 将技能绑定到当前租户内已发布且启用的智能体。 */
    @Transactional(rollbackFor = Exception.class)
    public SkillAuthorizationViews.Relation bindAgent(String skillId, SkillAuthorizationRequest.BindAgent request) {
        String tenantId = currentTenantId();
        Skill skill = requireSkill(skillId, tenantId);
        AgentConfig agent = requireBindableAgent(request.agentId(), tenantId);
        SkillAgentRelation relation = relations.findByTenantIdAndSkillIdAndDeletedFalse(tenantId, skill.getId())
                .orElseGet(() -> newRelation(tenantId, skill.getId()));
        relation.setAgentId(agent.getId());
        relation.setEnabled(true);
        relations.save(relation);
        return SkillAuthorizationViews.Relation.from(skill, agent);
    }

    /** 解除技能与智能体的绑定；未绑定时保持成功且不产生副作用。 */
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindAgent(String skillId) {
        String tenantId = currentTenantId();
        requireSkill(skillId, tenantId);
        relations.findByTenantIdAndSkillIdAndDeletedFalse(tenantId, skillId.trim()).ifPresent(relations::delete);
        return true;
    }

    /** 查询用户在当前租户拥有的技能白名单。 */
    public List<String> userSkillIds(String userId) {
        return userPermissions.findEnabledSkillIds(currentTenantId(), requiredId(userId, "用户ID不能为空"));
    }

    /** 使用完整技能集合替换一个用户在当前租户的技能白名单。 */
    @Transactional(rollbackFor = Exception.class)
    public List<String> replaceUserSkillIds(String userId, SkillAuthorizationRequest.ReplaceUserSkills request) {
        String tenantId = currentTenantId();
        String normalizedUserId = requiredId(userId, "用户ID不能为空");
        List<String> skillIds = normalizedIds(request.skillIds(), "技能ID不能为空");
        verifySkills(tenantId, skillIds);

        List<UserSkillPermission> currentPermissions = userPermissions
                .findByTenantIdAndUserIdAndDeletedFalse(tenantId, normalizedUserId);
        if (!currentPermissions.isEmpty()) {
            userPermissions.deleteAll(currentPermissions);
            userPermissions.flush();
        }

        List<UserSkillPermission> replacements = skillIds.stream()
                .map(skillId -> newUserPermission(tenantId, normalizedUserId, skillId))
                .toList();
        if (!replacements.isEmpty()) userPermissions.saveAll(replacements);
        return skillIds;
    }

    private Specification<Skill> tenantSpecification(String tenantId) {
        return (root, query, builder) -> builder.and(
                builder.equal(root.get("tenantId"), tenantId),
                builder.isFalse(root.get("deleted")));
    }

    private SkillAgentRelation newRelation(String tenantId, String skillId) {
        SkillAgentRelation relation = new SkillAgentRelation();
        relation.setTenantId(tenantId);
        relation.setSkillId(skillId);
        relation.setEnabled(true);
        relation.setSortOrder(Skill.DEFAULT_SORT_ORDER);
        return relation;
    }

    private UserSkillPermission newUserPermission(String tenantId, String userId, String skillId) {
        UserSkillPermission permission = new UserSkillPermission();
        permission.setTenantId(tenantId);
        permission.setUserId(userId);
        permission.setSkillId(skillId);
        permission.setEnabled(true);
        permission.setSortOrder(Skill.DEFAULT_SORT_ORDER);
        return permission;
    }

    private Skill requireSkill(String skillId, String tenantId) {
        return skills.findByIdAndTenantIdAndDeletedFalse(requiredId(skillId, "技能ID不能为空"), tenantId)
                .orElseThrow(() -> BizException.notFound("AI技能"));
    }

    private AgentConfig requireBindableAgent(String agentId, String tenantId) {
        AgentConfig agent = agents.findByIdAndTenantIdAndDeletedFalse(requiredId(agentId, "智能体ID不能为空"), tenantId)
                .orElseThrow(() -> BizException.notFound("智能体定义"));
        if (!Boolean.TRUE.equals(agent.getEnabled()) || agent.getPublishState() != AgentPublishState.PUBLISHED) {
            throw new BizException("只能绑定已发布且启用的智能体");
        }
        return agent;
    }

    private void verifySkills(String tenantId, Collection<String> skillIds) {
        if (skillIds.isEmpty()) return;
        if (skills.findByIdInAndTenantIdAndDeletedFalse(skillIds, tenantId).size() != skillIds.size()) {
            throw new BizException("技能不存在或不属于当前租户");
        }
    }

    private List<String> normalizedIds(List<String> ids, String message) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String id : ids) values.add(requiredId(id, message));
        return List.copyOf(values);
    }

    private String currentTenantId() {
        String tenantId = SecurityUtils.getTenantId();
        if (!StringUtils.hasText(tenantId) || "0".equals(tenantId)) {
            throw new BizException("未获取到有效租户上下文");
        }
        return tenantId.trim();
    }

    private String requiredId(String value, String message) {
        if (!StringUtils.hasText(value)) throw new BizException(message);
        return value.trim();
    }
}
