package com.ai.api.agent.config;

import com.ai.api.agent.AgentApiHeaders;
import com.ai.api.agent.client.AgentClient;
import com.ai.api.agent.dto.AgentInvokeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AgentClientContractTest {

    @Test
    void invokeContractShouldRequireExplicitIdempotencyKeyAndForwardItAsHeader() throws Exception {
        assertThat(AgentClient.class.getMethod(
                "invoke", String.class, String.class, AgentInvokeRequest.class)).isNotNull();

        Method invoke = AgentClientConfig.InternalAgentClient.class.getDeclaredMethod(
                "invoke", String.class, String.class, String.class, AgentInvokeRequest.class);
        RequestHeader idempotencyKeyHeader = invoke.getParameters()[2].getAnnotation(RequestHeader.class);

        assertThat(idempotencyKeyHeader).isNotNull();
        assertThat(idempotencyKeyHeader.value()).isEqualTo(AgentApiHeaders.IDEMPOTENCY_KEY);
        assertThat(idempotencyKeyHeader.required()).isTrue();
    }
}
