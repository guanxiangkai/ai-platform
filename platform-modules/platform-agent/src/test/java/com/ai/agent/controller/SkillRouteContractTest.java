package com.ai.agent.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SkillRouteContractTest {
    @Test
    void shouldExposeWebsiteSkillDisplayRoutesFromGenericAgentService() throws NoSuchMethodException {
        assertThat(SkillController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/agent/skill");
        assertThat(SkillController.class.getMethod("display", String.class)
                .getAnnotation(GetMapping.class).value()).containsExactly("/display");

        assertThat(SkillScopeTypeController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/agent/skill-scope-type");
        assertThat(SkillScopeTypeController.class.getMethod("display")
                .getAnnotation(GetMapping.class).value()).containsExactly("/display");
    }

    @Test
    void shouldExposeCompleteManagementSkillContract() {
        assertThat(Arrays.stream(SkillController.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName))
                .contains("page", "detail", "create", "update", "delete", "pin", "unpin");
    }
}
