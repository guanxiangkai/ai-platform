package com.ai.agent.integration;

import com.ai.agent.domain.AgentProviderType;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 按协议选择唯一提供方适配器。 */
@Component
public class AgentProviderRegistry {
    private final Map<AgentProviderType, AgentProviderClient> clients;

    /** 创建适配器注册表并拒绝重复协议实现。 */
    public AgentProviderRegistry(List<AgentProviderClient> values) {
        Map<AgentProviderType, AgentProviderClient> resolved = new EnumMap<>(AgentProviderType.class);
        values.forEach(value -> {
            if (resolved.put(value.providerType(), value) != null) {
                throw new IllegalStateException("智能体提供方协议存在重复实现: " + value.providerType());
            }
        });
        this.clients = Map.copyOf(resolved);
    }

    /** 返回指定协议适配器。 */
    public AgentProviderClient require(AgentProviderType type) {
        AgentProviderClient client = clients.get(type);
        if (client == null) throw new BizException("当前环境不支持智能体协议: " + type);
        return client;
    }
}
