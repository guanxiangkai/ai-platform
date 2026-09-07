package com.ai.system.controller;

import com.ai.system.domain.dto.UserDTO;
import com.ai.system.service.IUserService;
import io.github.guanxiangkai.web.plus.web.properties.ImportProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserPasswordRequestTest {

    private IUserService users;
    private WebTestClient client;
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        users = mock(IUserService.class);
        context = new AnnotationConfigApplicationContext();
        context.register(WebConfiguration.class);
        context.registerBean(ImportProperties.class);
        context.registerBean(UserController.class, () -> new UserController(users));
        context.refresh();
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    @Test
    void updateShouldNotSilentlyIgnoreAnAlternativePasswordField() {
        client.put().uri("/system/user/user-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"test-user","passwordDigest":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
                        """)
                .exchange().expectStatus().isBadRequest();

        verifyNoInteractions(users);
    }

    @Test
    void updateShouldPassPasswordFieldToTheUserService() {
        client.put().uri("/system/user/user-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"password":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
                        """)
                .exchange().expectStatus().isOk();

        var request = ArgumentCaptor.forClass(UserDTO.class);
        verify(users).update(eq("user-1"), request.capture());
        assertThat(request.getValue().password()).isEqualTo("a".repeat(40));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebFlux
    static class WebConfiguration {
    }
}
