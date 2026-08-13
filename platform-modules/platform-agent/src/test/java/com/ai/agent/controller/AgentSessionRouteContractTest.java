package com.ai.agent.controller;

import com.ai.agent.domain.dto.AgentSessionAskRequest;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresLogin;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** 用户端智能体会话与语音路由契约测试。 */
class AgentSessionRouteContractTest {
    @Test
    void shouldExposeAuthenticatedSessionAskRoute() throws NoSuchMethodException {
        assertThat(AgentSessionController.class.getAnnotation(RequiresLogin.class)).isNotNull();
        assertThat(AgentSessionController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/agent/session");

        Method ask = AgentSessionController.class.getMethod("ask", AgentSessionAskRequest.class);
        assertThat(ask.getAnnotation(PostMapping.class).value()).containsExactly("/ask");
    }

    @Test
    void shouldExposeAuthenticatedMultipartSpeechRoute() throws NoSuchMethodException {
        assertThat(AgentSpeechController.class.getAnnotation(RequiresLogin.class)).isNotNull();
        assertThat(AgentSpeechController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/agent/speech");

        Method transcribe = AgentSpeechController.class.getMethod("transcribe", Mono.class, String.class);
        PostMapping mapping = transcribe.getAnnotation(PostMapping.class);
        assertThat(mapping.value()).containsExactly("/transcribe");
        assertThat(mapping.consumes()).containsExactly(MediaType.MULTIPART_FORM_DATA_VALUE);
    }
}
