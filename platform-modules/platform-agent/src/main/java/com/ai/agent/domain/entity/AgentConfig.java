package com.ai.agent.domain.entity;

import com.ai.agent.domain.AgentInvocationMode;
import com.ai.agent.domain.AgentProviderType;
import com.ai.agent.domain.AgentPublishState;
import io.github.guanxiangkai.jpa.plus.audit.annotation.AuditExclude;
import io.github.guanxiangkai.jpa.plus.field.encrypt.annotation.Encrypt;
import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 租户自主维护的通用智能体定义。
 *
 * <p>该模型只描述运行协议与模型参数，不出现产品、部门或具体业务对象。</p>
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_agent_config", comment = "智能体定义与发布配置表")
public class AgentConfig extends DataTenantEntity {
    @Column(name = "agent_code", nullable = false, length = 128)
    private String agentCode;

    @Column(name = "agent_name", nullable = false, length = 256)
    private String agentName;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 32)
    private AgentProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "invocation_mode", nullable = false, length = 32)
    private AgentInvocationMode invocationMode;

    @Column(name = "endpoint_url", nullable = false, length = 1000)
    private String endpointUrl;

    @Column(name = "model_name", length = 256)
    private String modelName;

    @AuditExclude
    @Encrypt
    @Column(name = "credential", length = 2000)
    private String credential;

    @Column(name = "system_prompt", columnDefinition = "text")
    private String systemPrompt;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "runtime_config", columnDefinition = "text")
    private String runtimeConfig;

    @Column(name = "owner_user_id", length = 64)
    private String ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_state", nullable = false, length = 32)
    private AgentPublishState publishState = AgentPublishState.DRAFT;

    @Column(name = "revision", nullable = false)
    private Integer revision = 1;
}
