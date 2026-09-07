package com.ai.auth.controller;

import com.ai.auth.domain.dto.LoginRequest;
import com.ai.auth.service.IAuthService;
import io.github.guanxiangkai.web.plus.core.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {

    private IAuthService authService;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(IAuthService.class);
        webTestClient = WebTestClient.bindToController(new AuthController(authService)).build();
    }

    @Test
    void shouldReturnBusinessFailureResponseWhenLoginFails() {
        LoginRequest request = new LoginRequest("admin", "a".repeat(40), null, null);
        Mockito.when(authService.login(Mockito.eq(request), Mockito.any()))
                .thenReturn(Mono.error(new BaseException.BusinessException("用户名或密码错误")));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "username": "admin",
                          "password": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("用户名或密码错误");
    }

    @Test
    void shouldSupportApiAuthLoginPrefix() {
        LoginRequest request = new LoginRequest("admin", "a".repeat(40), null, null);
        Mockito.when(authService.login(Mockito.eq(request), Mockito.any()))
                .thenReturn(Mono.error(new BaseException.BusinessException("用户名或密码错误")));

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "username": "admin",
                          "password": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("用户名或密码错误");
    }

    @Test
    void shouldRejectRawPasswordBeforeCallingAuthService() {
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        { "username": "admin", "password": "raw-password" }
                        """)
                .exchange()
                .expectStatus().isBadRequest();

        Mockito.verifyNoInteractions(authService);
    }

    @Test
    void shouldRequirePasswordFieldInsteadOfDigestAlias() {
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"admin","passwordDigest":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
        Mockito.verifyNoInteractions(authService);
    }

    @Test
    void passwordShouldBeWriteOnlyAndExcludedFromLogs() {
        String password = "a".repeat(40);
        var mapper = JsonMapper.builder().build();
        LoginRequest request = mapper.readValue(
                "{\"username\":\"admin\",\"password\":\"" + password + "\"}", LoginRequest.class);

        assertThat(request.password()).isEqualTo(password);
        assertThat(mapper.writeValueAsString(request)).doesNotContain(password, "\"password\"");
        assertThat(request.toString()).doesNotContain(password).contains("password=[REDACTED]");
    }
}
