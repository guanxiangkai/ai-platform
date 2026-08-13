package com.ai.agent.service;

import com.ai.agent.domain.CodedEnum;
import com.ai.agent.config.AgentInvocationProperties;
import com.ai.agent.domain.SkillJumpType;
import com.ai.agent.domain.SkillTerminalType;
import com.ai.agent.domain.SkillType;
import com.ai.agent.domain.dto.SkillRequest;
import com.ai.agent.domain.entity.Skill;
import com.ai.agent.domain.vo.SkillViews;
import com.ai.agent.repository.SkillRepository;
import com.ai.agent.repository.SkillScopeTypeRepository;
import com.ai.agent.repository.UserSkillPermissionRepository;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.error.exception.PermissionDeniedException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 通用技能的查询与管理服务。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SkillManagementService {
    private final SkillRepository skills;
    private final SkillScopeTypeRepository scopeTypes;
    private final UserSkillPermissionRepository userSkillPermissions;
    private final AgentInvocationProperties invocationProperties;

    /** 分页查询当前租户技能。 */
    public PageResponse<SkillViews.Item> page(
            int pageNum, int pageSize, String title, String type, String terminalType) {
        String tenantId = currentTenantId();
        SkillType normalizedType = optionalEnum(SkillType.class, type, "技能来源类型无效");
        SkillTerminalType normalizedTerminal = optionalEnum(
                SkillTerminalType.class, terminalType, "终端类型无效");
        Specification<Skill> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantId"), tenantId));
            predicates.add(builder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(title)) {
                String pattern = "%" + title.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("actionTitle")), pattern),
                        builder.like(builder.lower(root.get("actionPrompt")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)));
            }
            if (normalizedType != null) {
                predicates.add(builder.equal(root.get("skillType"), normalizedType));
            }
            if (normalizedTerminal != null) {
                predicates.add(builder.equal(root.get("terminalType"), normalizedTerminal));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        int safePage = Math.max(1, pageNum);
        int safeSize = Math.min(1000, Math.max(1, pageSize));
        Sort sort = Sort.by(Sort.Order.desc("pinned"), Sort.Order.asc("sortInfo.sortOrder"),
                Sort.Order.desc("weight"), Sort.Order.desc("createTime"));
        Page<Skill> result = skills.findAll(specification, PageRequest.of(safePage - 1, safeSize, sort));
        return PageResponse.of(result.getContent().stream().map(SkillViews.Item::from).toList(),
                result.getTotalElements(), safePage, safeSize);
    }

    /** 查询当前租户指定终端的启用技能。 */
    public List<SkillViews.Item> display(String terminalType) {
        String tenantId = currentTenantId();
        SkillTerminalType terminal = requiredEnum(SkillTerminalType.class, terminalType, "终端类型无效");
        List<String> authorizedSkillIds = null;
        if (terminal.isWebsite()) {
            authorizedSkillIds = userSkillPermissions.findEnabledSkillIds(tenantId, currentUserId());
            if (authorizedSkillIds.isEmpty()) return List.of();
        } else if (!SecurityUtils.isSuperAdmin()
                && !SecurityUtils.hasAnyPermission("agent:skill:list")) {
            throw new PermissionDeniedException("无权查看管理端技能");
        }
        List<String> skillIds = authorizedSkillIds;
        Specification<Skill> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantId"), tenantId));
            predicates.add(builder.isFalse(root.get("deleted")));
            predicates.add(builder.isTrue(root.get("enabled")));
            predicates.add(builder.equal(root.get("terminalType"), terminal));
            if (skillIds != null) predicates.add(root.get("id").in(skillIds));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Sort sort = Sort.by(Sort.Order.desc("pinned"), Sort.Order.asc("sortInfo.sortOrder"),
                Sort.Order.desc("weight"), Sort.Order.asc("actionTitle"));
        return skills.findAll(specification, PageRequest.of(0, invocationProperties.getSkillSelectionLimit(), sort))
                .getContent().stream().map(SkillViews.Item::from).toList();
    }

    /** 查询单个技能，强制执行租户隔离。 */
    public SkillViews.Item detail(String id) {
        return SkillViews.Item.from(requireSkill(id));
    }

    /** 创建技能。 */
    @Transactional(rollbackFor = Exception.class)
    public SkillViews.Item create(SkillRequest request) {
        Skill skill = new Skill();
        skill.setTenantId(currentTenantId());
        skill.setOwnerId(currentUserId());
        skill.setPinned(false);
        skill.setEnabled(true);
        apply(skill, request);
        return SkillViews.Item.from(skills.save(skill));
    }

    /** 更新技能；置顶状态由独立接口维护。 */
    @Transactional(rollbackFor = Exception.class)
    public SkillViews.Item update(String id, SkillRequest request) {
        Skill skill = requireSkill(id);
        apply(skill, request);
        return SkillViews.Item.from(skills.save(skill));
    }

    /** 软删除技能。 */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String id) {
        skills.delete(requireSkill(id));
        return true;
    }

    /** 置顶技能。 */
    @Transactional(rollbackFor = Exception.class)
    public SkillViews.Item pin(String id) {
        return changePinned(id, true);
    }

    /** 取消置顶技能。 */
    @Transactional(rollbackFor = Exception.class)
    public SkillViews.Item unpin(String id) {
        return changePinned(id, false);
    }

    /** 查询当前租户允许出现在交互输入框中的作用域。 */
    public List<SkillViews.ScopeType> displayScopeTypes() {
        return scopeTypes
                .findByTenantIdAndDeletedFalseAndEnabledTrueAndDisplayInComposerTrue(currentTenantId(),
                        PageRequest.of(0, invocationProperties.getSkillSelectionLimit(),
                                Sort.by(Sort.Order.desc("weight"), Sort.Order.asc("typeCode"))))
                .getContent()
                .stream()
                .map(SkillViews.ScopeType::from)
                .toList();
    }

    private SkillViews.Item changePinned(String id, boolean pinned) {
        Skill skill = requireSkill(id);
        skill.setPinned(pinned);
        return SkillViews.Item.from(skills.save(skill));
    }

    private Skill requireSkill(String id) {
        return skills.findByIdAndTenantIdAndDeletedFalse(id, currentTenantId())
                .orElseThrow(() -> BizException.notFound("AI技能"));
    }

    private void apply(Skill skill, SkillRequest request) {
        String scopeTypeCode = StringUtils.hasText(request.scopeTypeCode())
                ? request.scopeTypeCode().trim().toUpperCase(Locale.ROOT) : Skill.DEFAULT_SCOPE_TYPE_CODE;
        if (!scopeTypes.existsByTenantIdAndTypeCodeAndDeletedFalseAndEnabledTrue(
                skill.getTenantId(), scopeTypeCode)) {
            throw new BizException("技能作用域类型不存在或已停用");
        }

        SkillJumpType jumpType = request.jumpType();
        String jumpUrl = trimToNull(request.jumpUrl());
        if (request.actionType().isJump()) {
            if (!StringUtils.hasText(jumpUrl)) throw new BizException("跳转类技能必须配置跳转地址");
            jumpType = jumpType == null ? SkillJumpType.INTERNAL : jumpType;
            validateJumpUrl(jumpType, jumpUrl);
        } else {
            jumpType = null;
            jumpUrl = null;
        }

        skill.setDescription(required(request.desc()));
        skill.setActionType(request.actionType());
        skill.setActionTitle(required(request.actionTitle()));
        skill.setActionPrompt(required(request.actionPrompt()));
        skill.setSkillType(request.type());
        skill.setScopeTypeCode(scopeTypeCode);
        skill.setTerminalType(request.terminalType());
        skill.setJumpType(jumpType);
        skill.setJumpUrl(jumpUrl);
        skill.setDisplayMode(request.displayMode());
        skill.setSortOrder(request.sortOrder() == null ? Skill.DEFAULT_SORT_ORDER : request.sortOrder());
        skill.setWeight(request.weight() == null ? Skill.DEFAULT_WEIGHT : request.weight());
        skill.setRemark(trimToNull(request.remark()));
    }

    private void validateJumpUrl(SkillJumpType jumpType, String jumpUrl) {
        if (jumpType.isInternal()) {
            if (!jumpUrl.startsWith("/")) throw new BizException("内部跳转地址必须以 / 开头");
            return;
        }
        try {
            URI uri = new URI(jumpUrl);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || !StringUtils.hasText(uri.getHost())) {
                throw new BizException("外部跳转地址必须是完整的 HTTP 或 HTTPS 地址");
            }
        } catch (URISyntaxException exception) {
            throw new BizException("外部跳转地址格式无效");
        }
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

    private <E extends Enum<E> & CodedEnum> E requiredEnum(Class<E> type, String code, String message) {
        try {
            return CodedEnum.required(type, code);
        } catch (IllegalArgumentException exception) {
            throw new BizException(message);
        }
    }

    private <E extends Enum<E> & CodedEnum> E optionalEnum(Class<E> type, String code, String message) {
        try {
            return CodedEnum.optional(type, code);
        } catch (IllegalArgumentException exception) {
            throw new BizException(message);
        }
    }

    private String required(String value) {
        if (!StringUtils.hasText(value)) throw new BizException("必填字段不能为空");
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
