package com.ai.auth.service.impl;

import com.ai.auth.crypto.RsaJwtServiceImpl;
import com.ai.auth.domain.dto.LoginRequest;
import com.ai.auth.properties.AuthSuperAdminProperties;
import com.ai.auth.properties.JwtProperties;
import com.ai.auth.service.AuthProtectionService;
import io.github.guanxiangkai.web.plus.security.password.PasswordProtocol;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    @Test
    @SuppressWarnings("unchecked")
    void loginShouldUseConfiguredSuperAdminDigestFromAuthConfigOnly() {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RsaJwtServiceImpl jwtService = mock(RsaJwtServiceImpl.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        AuthProtectionService authProtectionService = mock(AuthProtectionService.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/login").build());

        String password = PasswordProtocol.sha1Utf8("platform-password");
        when(jwtService.generateAccessToken(eq("platform-super-admin"), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(eq("platform-super-admin"), any())).thenReturn("refresh-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("platform:auth:super-admin:token-version")).thenReturn(1L);
        when(jwtProperties.getAccessTokenExpirationSeconds()).thenReturn(7200L);
        when(jwtProperties.getRefreshTokenExpirationSeconds()).thenReturn(86400L);

        AuthServiceImpl authService = new AuthServiceImpl(
                passwordEncoder,
                jwtService,
                jwtProperties,
                new AuthSuperAdminProperties(true, "platform-admin", password),
                redisTemplate,
                authProtectionService
        );

        var response = authService.login(new LoginRequest("platform-admin", password, null, null), exchange).block();

        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(jwtService).generateAccessToken(eq("platform-super-admin"), claimsCaptor.capture());
        assertThat(claimsCaptor.getValue()).containsEntry("superAdmin", true);
        assertThat(claimsCaptor.getValue()).containsEntry(AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION, 1L);
        assertThat(claimsCaptor.getValue()).doesNotContainKeys("permissions", "roles", "posts", "deptIds");
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo("platform-super-admin");
        assertThat(response.username()).isEqualTo("platform-admin");
        assertThat(response.userType()).isEqualTo("SUPER_ADMIN");
        assertThat(response.superAdmin()).isTrue();
        assertThat(response.permissions()).containsExactly("*");
        assertThat(response.roleCodes()).isEmpty();
        assertThat(response.postCodes()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginShouldVerifyConfiguredSuperAdminWithoutPasswordEncoder() {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RsaJwtServiceImpl jwtService = mock(RsaJwtServiceImpl.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        AuthProtectionService authProtectionService = mock(AuthProtectionService.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/login").build());

        when(jwtService.generateAccessToken(eq("platform-super-admin"), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(eq("platform-super-admin"), any())).thenReturn("refresh-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("platform:auth:super-admin:token-version")).thenReturn(1L);
        when(jwtProperties.getAccessTokenExpirationSeconds()).thenReturn(7200L);
        when(jwtProperties.getRefreshTokenExpirationSeconds()).thenReturn(86400L);

        String configuredDigest = digest();
        AuthServiceImpl authService = new AuthServiceImpl(
                passwordEncoder,
                jwtService,
                jwtProperties,
                new AuthSuperAdminProperties(true, "admin", configuredDigest),
                redisTemplate,
                authProtectionService
        );

        var response = authService.login(new LoginRequest("admin", configuredDigest, null, null), exchange).block();

        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(jwtService).generateAccessToken(eq("platform-super-admin"), claimsCaptor.capture());
        assertThat(claimsCaptor.getValue()).containsEntry("superAdmin", true);
        assertThat(response).isNotNull();
        assertThat(response.superAdmin()).isTrue();
        org.mockito.Mockito.verifyNoInteractions(passwordEncoder);
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginShouldIgnoreCacheSuperAdminWhenConfiguredAccountDoesNotMatch() {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RsaJwtServiceImpl jwtService = mock(RsaJwtServiceImpl.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        AuthProtectionService authProtectionService = mock(AuthProtectionService.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/alpha/auth/login")
                        .header(AuthConstants.HeaderConstants.TENANT_ID, "tenant-1")
                        .build()
        );

        Map<Object, Object> authHash = new HashMap<>();
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_ID, "2");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_USERNAME, "tenant-user");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_PASSWORD_HASH, validBcryptHash());
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_ENABLED, "true");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION, "1");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_NICKNAME, "租户用户");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_USER_TYPE, "ADMIN");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_SUPER_ADMIN, "true");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_TENANT_ID, "tenant-1");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_DEPT_ID, "dept-1");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_ROLE_CODES, "[\"member\"]");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_POST_CODES, "[\"worker\"]");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_PERMISSIONS, "[\"home:view\"]");
        authHash.put(AuthConstants.UserAuthCacheConstants.FIELD_DEPT_IDS, "[\"dept-1\"]");

        when(passwordEncoder.matches(digest(), validBcryptHash())).thenReturn(true);
        when(jwtService.generateAccessToken(eq("2"), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(eq("2"), any())).thenReturn("refresh-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.get(
                AuthConstants.UserAuthCacheConstants.USERNAME_INDEX_PREFIX + "tenant-1:tenant-user"))
                .thenReturn("2");
        when(hashOperations.entries(AuthConstants.UserAuthCacheConstants.USER_AUTH_CACHE_PREFIX + "2")).thenReturn(authHash);
        when(hashOperations.increment(
                AuthConstants.UserAuthCacheConstants.USER_AUTH_CACHE_PREFIX + "2",
                AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION,
                1
        )).thenReturn(2L);
        when(jwtProperties.getAccessTokenExpirationSeconds()).thenReturn(7200L);
        when(jwtProperties.getRefreshTokenExpirationSeconds()).thenReturn(86400L);

        AuthServiceImpl authService = new AuthServiceImpl(
                passwordEncoder,
                jwtService,
                jwtProperties,
                new AuthSuperAdminProperties(true, "admin", digest()),
                redisTemplate,
                authProtectionService
        );

        var response = authService.login(new LoginRequest("tenant-user", digest(), null, null), exchange).block();

        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(jwtService).generateAccessToken(eq("2"), claimsCaptor.capture());
        assertThat(claimsCaptor.getValue()).containsEntry("superAdmin", false);
        assertThat(response).isNotNull();
        assertThat(response.superAdmin()).isFalse();
        assertThat(response.roleCodes()).containsExactly("member");
        assertThat(response.postCodes()).containsExactly("worker");
    }

    @Test
    void loginShouldRejectUnrelatedCredentialAsPlatformSuperAdminCredential() {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RsaJwtServiceImpl jwtService = mock(RsaJwtServiceImpl.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        AuthProtectionService authProtectionService = mock(AuthProtectionService.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/login").build());
        String passwordDigest = digest();

        AuthServiceImpl authService = new AuthServiceImpl(
                passwordEncoder,
                jwtService,
                jwtProperties,
                new AuthSuperAdminProperties(true, "platform-admin", passwordDigest),
                redisTemplate,
                authProtectionService
        );

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("platform-admin", "b".repeat(40), null, null), exchange).block())
                .hasMessageContaining("用户名或密码错误");
        org.mockito.Mockito.verifyNoInteractions(passwordEncoder);
    }

    @Test
    void loginShouldRejectEmptyUppercaseAndBcryptSuperAdminCredentials() {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthServiceImpl authService = new AuthServiceImpl(
                passwordEncoder,
                mock(RsaJwtServiceImpl.class),
                mock(JwtProperties.class),
                new AuthSuperAdminProperties(true, "platform-admin", digest()),
                mock(StringRedisTemplate.class),
                mock(AuthProtectionService.class)
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/login").build());

        for (String credential : new String[]{"", "A".repeat(40), "$2b$12$" + "A".repeat(53)}) {
            assertThatThrownBy(() -> authService.login(
                    new LoginRequest("platform-admin", credential, null, null), exchange).block())
                    .hasMessageContaining("用户名或密码错误");
        }
        org.mockito.Mockito.verifyNoInteractions(passwordEncoder);
    }

    private static String digest() {
        return "a".repeat(40);
    }

    private static String validBcryptHash() {
        return "$2b$12$" + "A".repeat(53);
    }
}
