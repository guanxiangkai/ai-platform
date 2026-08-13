package com.ai.agent.controller;

import com.ai.agent.domain.dto.SkillAuthorizationRequest;
import io.github.guanxiangkai.web.plus.security.annotation.RequiresPermission;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** 技能授权接口与权限边界契约测试。 */
class SkillAuthorizationRouteContractTest {
    @Test
    void shouldExposeTenantScopedAuthorizationRoutesWithExplicitPermissions() throws NoSuchMethodException {
        assertThat(SkillAuthorizationController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/agent/skill-authorization");
        assertRoute(SkillAuthorizationController.class.getMethod("relations"), GetMapping.class,
                "/relations", "agent:authorization:list");
        assertRoute(SkillAuthorizationController.class.getMethod("bind", String.class,
                        SkillAuthorizationRequest.BindAgent.class), PutMapping.class,
                "/relations/{skillId}", "agent:authorization:edit");
        assertRoute(SkillAuthorizationController.class.getMethod("unbind", String.class), DeleteMapping.class,
                "/relations/{skillId}", "agent:authorization:edit");
        assertRoute(SkillAuthorizationController.class.getMethod("userSkills", String.class), GetMapping.class,
                "/users/{userId}", "agent:authorization:list");
        assertRoute(SkillAuthorizationController.class.getMethod("replaceUserSkills", String.class,
                        SkillAuthorizationRequest.ReplaceUserSkills.class), PutMapping.class,
                "/users/{userId}", "agent:authorization:edit");
    }

    @Test
    void shouldExposePublishedAgentOptionsForAuthorizationPanel() throws NoSuchMethodException {
        assertRoute(AgentManagementController.class.getMethod("options"), GetMapping.class,
                "/options", "agent:authorization:list");
    }

    private void assertRoute(
            Method method, Class<? extends java.lang.annotation.Annotation> mappingType,
        String path, String permission) {
        assertThat(routePath(method, mappingType)).containsExactly(path);
        assertThat(method.getAnnotation(RequiresPermission.class).value()).containsExactly(permission);
    }

    private String[] routePath(Method method, Class<? extends java.lang.annotation.Annotation> mappingType) {
        if (mappingType == GetMapping.class) return method.getAnnotation(GetMapping.class).value();
        if (mappingType == PutMapping.class) return method.getAnnotation(PutMapping.class).value();
        if (mappingType == DeleteMapping.class) return method.getAnnotation(DeleteMapping.class).value();
        throw new IllegalArgumentException("不支持的路由注解");
    }
}
