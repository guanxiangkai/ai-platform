package com.ai.auth.controller;

import com.ai.auth.domain.dto.LoginRequest;
import com.ai.auth.domain.vo.LoginResponse;
import com.ai.auth.service.IAuthService;
import io.github.guanxiangkai.web.plus.core.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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
        LoginRequest request = new LoginRequest("admin", "bad-password", null, null);
        Mockito.when(authService.login(Mockito.eq(request), Mockito.any()))
                .thenReturn(Mono.error(new BaseException.BusinessException("用户名或密码错误")));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "username": "admin",
                          "password": "bad-password"
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
        LoginRequest request = new LoginRequest("admin", "bad-password", null, null);
        Mockito.when(authService.login(Mockito.eq(request), Mockito.any()))
                .thenReturn(Mono.error(new BaseException.BusinessException("用户名或密码错误")));

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "username": "admin",
                          "password": "bad-password"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("用户名或密码错误");
    }

    @Test
    void shouldReadRefreshTokenFromJsonBody() {
        LoginResponse response = new LoginResponse(
                "new-access-token",
                "new-refresh-token",
                "Bearer",
                7200L,
                "user-1",
                "example-user",
                "Example User",
                null,
                "USER",
                false,
                Set.of("operator"),
                Set.of(),
                Set.of("system:read"),
                "dept-1",
                Set.of("dept-1"));
        Mockito.when(authService.refreshToken(eq("refresh-token-value"), any()))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "refreshToken": "refresh-token-value"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.data.accessToken").isEqualTo("new-access-token")
                .jsonPath("$.data.refreshToken").isEqualTo("new-refresh-token");

        verify(authService).refreshToken(eq("refresh-token-value"), any());
    }

    @Test
    void shouldReturnBusinessFailureResponseWhenRefreshFails() {
        Mockito.when(authService.refreshToken(eq("expired-refresh-token"), any()))
                .thenReturn(Mono.error(new BaseException.BusinessException("刷新令牌已失效")));

        webTestClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "refreshToken": "expired-refresh-token"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("刷新令牌已失效");
    }

    @Test
    void shouldRejectRefreshTokenInQueryString() {
        webTestClient.post()
                .uri("/auth/refresh?refreshToken=query-token")
                .exchange()
                .expectStatus().is4xxClientError();

        Mockito.verifyNoInteractions(authService);
    }
}
