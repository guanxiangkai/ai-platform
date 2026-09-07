package com.ai.system.controller;

import com.ai.system.domain.dto.UserDTO;
import com.ai.system.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserPasswordRequestTest {

    private IUserService users;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        users = mock(IUserService.class);
        client = WebTestClient.bindToController(new UserController(users)).build();
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
}
