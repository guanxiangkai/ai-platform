package com.ai.agent.service;

import com.ai.agent.config.AgentInvocationProperties;
import com.ai.agent.domain.AgentPublishState;
import com.ai.agent.domain.AgentResponseMode;
import com.ai.agent.domain.AgentTrustedVariable;
import com.ai.agent.domain.SkillTerminalType;
import com.ai.agent.domain.dto.AgentInvocationRequest;
import com.ai.agent.domain.dto.AgentSessionAskRequest;
import com.ai.agent.domain.entity.AgentConfig;
import com.ai.agent.domain.entity.Skill;
import com.ai.agent.domain.entity.SkillAgentRelation;
import com.ai.agent.domain.vo.AgentSessionAskResponse;
import com.ai.agent.domain.vo.AgentViews;
import com.ai.agent.repository.AgentConfigRepository;
import com.ai.agent.repository.SkillAgentRelationRepository;
import com.ai.agent.repository.SkillRepository;
import com.ai.agent.repository.UserSkillPermissionRepository;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.error.exception.PermissionDeniedException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端智能体会话编排与技能授权边界。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AgentSessionService {
    private final AgentInvocationService invocations;
    private final AgentConfigRepository agents;
    private final SkillRepository skills;
    private final SkillAgentRelationRepository relations;
    private final UserSkillPermissionRepository userPermissions;
    private final AgentInvocationProperties properties;

    /**
     * 使用当前用户获准的技能调用其绑定智能体。
     *
     * <p>未指定技能时只允许拥有直接调用权限的用户使用租户内唯一可用智能体，避免隐式选择错误模型。</p>
     */
    public AgentSessionAskResponse ask(AgentSessionAskRequest request) {
        String tenantId = currentTenantId();
        String userId = currentUserId();
        AgentConfig agent = StringUtils.hasText(request.skillId())
                ? resolveSkillAgent(tenantId, userId, request.skillId().trim())
                : resolveDirectAgent(tenantId);
        AgentViews.InvocationResult result = invocations.invoke(agent.getId(), new AgentInvocationRequest(
                trimToNull(request.invocationId()),
                trimToNull(request.sessionId()),
                request.content().trim(),
                null,
                trimToNull(request.scopeTag()),
                trimToNull(request.skillId()),
                variables(request, tenantId, userId)
        ));
        return new AgentSessionAskResponse(
                result.messageId(),
                result.invocationId(),
                result.sessionId(),
                result.text(),
                List.of(),
                List.of()
        );
    }

    private AgentConfig resolveSkillAgent(String tenantId, String userId, String skillId) {
        Skill skill = skills.findByIdAndTenantIdAndDeletedFalse(skillId, tenantId)
                .orElseThrow(() -> BizException.notFound("AI技能"));
        if (!Boolean.TRUE.equals(skill.getEnabled()) || skill.getTerminalType() != SkillTerminalType.WEBSITE) {
            throw new BizException("当前技能不可用于用户端会话");
        }
        if (!userPermissions.findEnabledSkillIds(tenantId, userId).contains(skillId)) {
            throw new PermissionDeniedException("无权使用当前技能");
        }
        SkillAgentRelation relation = relations
                .findByTenantIdAndSkillIdAndEnabledTrueAndDeletedFalse(tenantId, skillId)
                .orElseThrow(() -> new BizException("当前技能尚未绑定可调用智能体"));
        return agents.findByIdAndTenantIdAndDeletedFalse(relation.getAgentId(), tenantId)
                .orElseThrow(() -> BizException.notFound("智能体定义"));
    }

    private AgentConfig resolveDirectAgent(String tenantId) {
        if (!SecurityUtils.isSuperAdmin() && !SecurityUtils.hasAnyPermission("agent:invoke")) {
            throw new PermissionDeniedException("请选择已授权的技能后发起会话");
        }
        List<AgentConfig> available = agents
                .findByTenantIdAndEnabledTrueAndPublishStateAndDeletedFalse(tenantId,
                        AgentPublishState.PUBLISHED, PageRequest.of(0, 2, Sort.by("agentName").ascending())).getContent();
        if (available.isEmpty()) throw new BizException("当前租户尚未配置可调用智能体");
        if (available.size() > 1) throw new BizException("当前租户存在多个可调用智能体，请先选择技能");
        return available.getFirst();
    }

    private Map<String, Object> variables(AgentSessionAskRequest request, String tenantId, String userId) {
        if (request.variables().size() > properties.getMaxVariableCount()) {
            throw new BizException("智能体变量数量超过限制");
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        int[] totalSize = {0};
        request.variables().forEach((key, value) -> {
            if (!StringUtils.hasText(key) || value == null) return;
            String normalizedKey = key.trim();
            if (normalizedKey.length() > properties.getMaxVariableNameLength()) {
                throw new BizException("智能体变量名称过长");
            }
            if (AgentTrustedVariable.contains(normalizedKey)) return;
            validateVariable(value, 1, totalSize);
            totalSize[0] += normalizedKey.length();
            if (totalSize[0] > properties.getMaxVariableTotalSize()) {
                throw new BizException("智能体变量内容超过限制");
            }
            variables.put(normalizedKey, value);
        });
        variables.put(AgentTrustedVariable.TENANT_ID.key(), tenantId);
        variables.put(AgentTrustedVariable.USER_ID.key(), userId);
        put(variables, AgentTrustedVariable.DEPT_ID, SecurityUtils.getDeptId());
        variables.put(AgentTrustedVariable.DEPT_IDS.key(), SecurityUtils.getDeptIds().stream().sorted().toList());
        put(variables, AgentTrustedVariable.SKILL_ID, request.skillId());
        put(variables, AgentTrustedVariable.SCOPE_TAG, request.scopeTag());
        variables.put(AgentTrustedVariable.VOICE_INPUT_ACTIVE.key(), request.voiceInputActive());
        variables.put(AgentTrustedVariable.RESPONSE_MODE.key(), (request.responseMode() == null
                ? AgentResponseMode.CHAT : request.responseMode()).name());
        return Map.copyOf(variables);
    }

    private void validateVariable(Object value, int depth, int[] totalSize) {
        if (depth > properties.getMaxVariableDepth()) throw new BizException("智能体变量嵌套层级超过限制");
        if (value instanceof String text) {
            if (text.length() > properties.getMaxVariableTextLength()) {
                throw new BizException("智能体变量文本过长");
            }
            addVariableSize(totalSize, text.length());
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            addVariableSize(totalSize, String.valueOf(value).length());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            if (map.size() > properties.getMaxVariableCollectionSize()) {
                throw new BizException("智能体变量对象成员过多");
            }
            map.forEach((key, nested) -> {
                if (!(key instanceof String nestedKey) || !StringUtils.hasText(nestedKey)) {
                    throw new BizException("智能体变量对象名称无效");
                }
                if (nestedKey.length() > properties.getMaxVariableNameLength()) {
                    throw new BizException("智能体变量名称过长");
                }
                addVariableSize(totalSize, nestedKey.length());
                if (nested != null) validateVariable(nested, depth + 1, totalSize);
            });
            return;
        }
        if (value instanceof Iterable<?> values) {
            int count = 0;
            for (Object nested : values) {
                if (++count > properties.getMaxVariableCollectionSize()) {
                    throw new BizException("智能体变量数组成员过多");
                }
                if (nested != null) validateVariable(nested, depth + 1, totalSize);
            }
            return;
        }
        throw new BizException("智能体变量包含不支持的数据类型");
    }

    private void addVariableSize(int[] totalSize, int length) {
        totalSize[0] += length;
        if (totalSize[0] > properties.getMaxVariableTotalSize()) {
            throw new BizException("智能体变量内容超过限制");
        }
    }

    private void put(Map<String, Object> variables, AgentTrustedVariable variable, String value) {
        if (StringUtils.hasText(value)) variables.put(variable.key(), value.trim());
    }

    private String currentTenantId() {
        String tenantId = SecurityUtils.getTenantId();
        if (!StringUtils.hasText(tenantId) || "0".equals(tenantId)) {
            throw new BizException("未获取到有效租户上下文");
        }
        return tenantId.trim();
    }

    private String currentUserId() {
        String userId = SecurityUtils.getUserId();
        if (!StringUtils.hasText(userId)) throw new BizException("未获取到当前用户");
        return userId.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
