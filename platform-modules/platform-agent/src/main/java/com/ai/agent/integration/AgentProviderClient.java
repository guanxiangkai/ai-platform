package com.ai.agent.integration;

import com.ai.agent.domain.AgentProviderType;
import com.ai.agent.domain.entity.AgentConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 提供方协议适配器；产品语义不得进入该边界。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface AgentProviderClient {
    /** 当前适配器支持的协议类型。 */
    AgentProviderType providerType();

    /** 调用上游并返回统一结果。 */
    AgentProviderResult invoke(AgentConfig definition, AgentProviderInvocation invocation);

    /**
     * 流式调用上游；不支持增量协议的提供方输出单个完成事件。
     */
    default Flux<AgentProviderStreamEvent> stream(
            AgentConfig definition, AgentProviderInvocation invocation) {
        return Mono.fromCallable(() -> invoke(definition, invocation))
                .map(AgentProviderStreamEvent::complete)
                .flux();
    }
}
