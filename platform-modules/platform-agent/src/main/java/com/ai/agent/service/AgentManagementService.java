package com.ai.agent.service;

import com.ai.agent.domain.AgentVoiceRecognitionState;
import com.ai.agent.domain.AgentInvocationState;
import com.ai.agent.domain.AgentProviderType;
import com.ai.agent.domain.AgentPublishState;
import com.ai.agent.domain.AgentSessionState;
import com.ai.agent.domain.dto.AgentDefinitionRequest;
import com.ai.agent.domain.entity.AgentCallRecord;
import com.ai.agent.domain.entity.AgentConfig;
import com.ai.agent.domain.entity.AgentSessionRecord;
import com.ai.agent.domain.entity.AgentVoiceRecord;
import com.ai.agent.domain.vo.AgentViews;
import com.ai.agent.repository.AgentCallRecordRepository;
import com.ai.agent.repository.AgentConfigRepository;
import com.ai.agent.repository.AgentMessageRecordRepository;
import com.ai.agent.repository.AgentSessionRecordRepository;
import com.ai.agent.repository.AgentVoiceRecordRepository;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用智能体定义与只读运行记录的应用服务。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AgentManagementService {
    private final AgentConfigRepository configs;
    private final AgentSessionRecordRepository sessions;
    private final AgentMessageRecordRepository messages;
    private final AgentCallRecordRepository calls;
    private final AgentVoiceRecordRepository voices;
    private final ObjectMapper objectMapper;

    /** 按当前租户分页查询智能体定义。 */
    public PageResponse<AgentViews.Definition> definitions(
            int page, int size, String keyword, AgentProviderType providerType,
            AgentPublishState publishState, Boolean enabled) {
        String tenantId = currentTenantId();
        Specification<AgentConfig> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantId"), tenantId));
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("agentCode")), pattern),
                        builder.like(builder.lower(root.get("agentName")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)));
            }
            if (providerType != null) predicates.add(builder.equal(root.get("providerType"), providerType));
            if (publishState != null) predicates.add(builder.equal(root.get("publishState"), publishState));
            if (enabled != null) predicates.add(builder.equal(root.get("enabled"), enabled));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Page<AgentConfig> result = configs.findAll(specification, pageable(page, size));
        return page(result.map(AgentViews.Definition::from), page, size);
    }

    /** 查询当前租户的智能体定义。 */
    public AgentViews.Definition definition(String id) {
        return AgentViews.Definition.from(requireDefinition(id));
    }

    /** 创建产品无关的智能体定义。 */
    @Transactional(rollbackFor = Exception.class)
    public AgentViews.Definition create(AgentDefinitionRequest request) {
        String tenantId = currentTenantId();
        String code = normalized(request.agentCode());
        if (configs.existsByTenantIdAndAgentCodeAndDeletedFalse(tenantId, code)) {
            throw new BizException("当前租户已存在相同智能体编码");
        }
        AgentConfig config = new AgentConfig();
        config.setTenantId(tenantId);
        apply(config, request, false);
        return AgentViews.Definition.from(configs.save(config));
    }

    /** 更新智能体定义；密钥留空时保留原值。 */
    @Transactional(rollbackFor = Exception.class)
    public AgentViews.Definition update(String id, AgentDefinitionRequest request) {
        AgentConfig config = requireDefinition(id);
        String code = normalized(request.agentCode());
        if (configs.existsByTenantIdAndAgentCodeAndIdNotAndDeletedFalse(config.getTenantId(), code, id)) {
            throw new BizException("当前租户已存在相同智能体编码");
        }
        apply(config, request, true);
        config.setRevision(config.getRevision() == null ? 1 : config.getRevision() + 1);
        return AgentViews.Definition.from(configs.save(config));
    }

    /** 修改智能体运行开关。 */
    @Transactional(rollbackFor = Exception.class)
    public AgentViews.Definition changeEnabled(String id, boolean enabled) {
        AgentConfig config = requireDefinition(id);
        config.setEnabled(enabled);
        return AgentViews.Definition.from(configs.save(config));
    }

    /** 软删除智能体定义。 */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String id) {
        configs.delete(requireDefinition(id));
        return true;
    }

    /** 分页查询当前租户会话。 */
    public PageResponse<AgentViews.Session> sessionPage(
            int page, int size, String keyword, String agentId, AgentSessionState state) {
        String tenantId = currentTenantId();
        Specification<AgentSessionRecord> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantId"), tenantId));
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("sessionCode")), pattern),
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("agentCode")), pattern)));
            }
            if (StringUtils.hasText(agentId)) predicates.add(builder.equal(root.get("agentId"), agentId.trim()));
            if (state != null) predicates.add(builder.equal(root.get("sessionState"), state));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Page<AgentSessionRecord> result = sessions.findAll(specification, pageable(page, size));
        return page(result.map(value -> AgentViews.Session.from(value, List.of())), page, size);
    }

    /** 查询会话和有序消息。 */
    public AgentViews.Session session(String id) {
        String tenantId = currentTenantId();
        AgentSessionRecord session = sessions.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> BizException.notFound("智能体会话"));
        List<AgentViews.Message> values = messages.findBySessionIdAndTenantIdOrderBySequenceNoAsc(id, tenantId)
                .stream().map(AgentViews.Message::from).toList();
        return AgentViews.Session.from(session, values);
    }

    /** 分页查询不含密钥的调用审计。 */
    public PageResponse<AgentViews.Invocation> invocationPage(
            int page, int size, String keyword, String agentId, AgentInvocationState state) {
        String tenantId = currentTenantId();
        Specification<AgentCallRecord> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantId"), tenantId));
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("invocationCode")), pattern),
                        builder.like(builder.lower(root.get("agentCode")), pattern),
                        builder.like(builder.lower(root.get("modelName")), pattern)));
            }
            if (StringUtils.hasText(agentId)) predicates.add(builder.equal(root.get("agentId"), agentId.trim()));
            if (state != null) predicates.add(builder.equal(root.get("invocationState"), state));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Page<AgentCallRecord> result = calls.findAll(specification, pageable(page, size));
        return page(result.map(AgentViews.Invocation::from), page, size);
    }

    /** 分页查询语音转写审计。 */
    public PageResponse<AgentViews.Voice> voicePage(int page, int size, String keyword, String status) {
        String tenantId = currentTenantId();
        Specification<AgentVoiceRecord> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantId"), tenantId));
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("fileName")), pattern),
                        builder.like(builder.lower(root.get("transcript")), pattern)));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(builder.equal(root.get("recognitionStatus"), recognitionState(status)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Page<AgentVoiceRecord> result = voices.findAll(specification, pageable(page, size));
        return page(result.map(AgentViews.Voice::from), page, size);
    }

    private AgentVoiceRecognitionState recognitionState(String value) {
        try {
            return AgentVoiceRecognitionState.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BizException("语音转写状态无效");
        }
    }

    /** 返回当前租户内的定义实体，仅供同一服务内运行编排使用。 */
    public AgentConfig requireDefinition(String id) {
        return configs.findByIdAndTenantIdAndDeletedFalse(id, currentTenantId())
                .orElseThrow(() -> BizException.notFound("智能体定义"));
    }

    private void apply(AgentConfig config, AgentDefinitionRequest request, boolean updating) {
        config.setAgentCode(normalized(request.agentCode()));
        config.setAgentName(normalized(request.agentName()));
        config.setDescription(trimToNull(request.description()));
        config.setProviderType(request.providerType());
        config.setInvocationMode(request.invocationMode());
        config.setEndpointUrl(validateEndpoint(request.endpointUrl()));
        config.setModelName(trimToNull(request.modelName()));
        if (!updating || StringUtils.hasText(request.credential())) {
            config.setCredential(trimToNull(request.credential()));
        }
        config.setSystemPrompt(trimToNull(request.systemPrompt()));
        config.setTemperature(request.temperature() == null ? 0.7D : request.temperature());
        config.setRuntimeConfig(validateJson(request.runtimeConfig()));
        config.setPublishState(request.publishState() == null ? AgentPublishState.DRAFT : request.publishState());
        config.setEnabled(request.enabled() == null || request.enabled());
        config.setRemark(trimToNull(request.remark()));
        config.setOwnerUserId(config.getOwnerUserId() == null ? currentUserId() : config.getOwnerUserId());
        validateRunnable(config);
    }

    private void validateRunnable(AgentConfig config) {
        if (config.getProviderType() == AgentProviderType.OPENAI_COMPATIBLE
                && !StringUtils.hasText(config.getModelName())) {
            throw new BizException("OpenAI 兼容智能体必须配置模型名称");
        }
        if (config.getProviderType() == AgentProviderType.OPENAI_COMPATIBLE
                && config.getInvocationMode() != null
                && config.getInvocationMode().name().equals("WORKFLOW")) {
            throw new BizException("OpenAI Chat Completions 协议不支持工作流调用模式");
        }
        if (config.getPublishState() == AgentPublishState.PUBLISHED
                && !StringUtils.hasText(config.getCredential())) {
            throw new BizException("发布智能体前必须配置提供方密钥");
        }
    }

    private String validateEndpoint(String value) {
        String endpoint = normalized(value);
        try {
            URI uri = new URI(endpoint);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || !StringUtils.hasText(uri.getHost())) {
                throw new BizException("智能体接入地址必须是完整的 HTTP 或 HTTPS 地址");
            }
            return uri.toString();
        } catch (URISyntaxException exception) {
            throw new BizException("智能体接入地址格式无效");
        }
    }

    private String validateJson(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            JsonNode node = objectMapper.readTree(value);
            if (!node.isObject()) throw new BizException("运行扩展配置必须是 JSON 对象");
            return objectMapper.writeValueAsString(node);
        } catch (JacksonException exception) {
            throw new BizException("运行扩展配置不是合法 JSON");
        }
    }

    private PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(1, page) - 1, Math.min(200, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createTime"));
    }

    private <T> PageResponse<T> page(Page<T> result, int page, int size) {
        return PageResponse.of(result.getContent(), result.getTotalElements(), Math.max(1, page),
                Math.min(200, Math.max(1, size)));
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

    private String normalized(String value) {
        if (!StringUtils.hasText(value)) throw new BizException("必填字段不能为空");
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
