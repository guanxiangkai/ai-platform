package com.ai.gateway.filter;

import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import com.ai.gateway.config.AiGatewayProperties;
import com.ai.gateway.util.GatewayPathMatcher;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayJwtAuthFilterTest {

    @Test
    void apiAuthLoginIsWhitelisted() {
        GatewayJwtAuthFilter filter = new GatewayJwtAuthFilter(new AiGatewayProperties(), mock(ReactiveStringRedisTemplate.class));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login").build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void tenantApiCryptoConfigIsWhitelistedBeforeLogin() {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setExcludePaths(java.util.List.of(
                "/alpha/web-plus/api-crypto/config",
                "/alpha/api/web-plus/api-crypto/config",
                "/beta/web-plus/api-crypto/config",
                "/beta/api/web-plus/api-crypto/config"
        ));
        GatewayJwtAuthFilter filter = new GatewayJwtAuthFilter(properties, mock(ReactiveStringRedisTemplate.class));

        for (String path : java.util.List.of(
                "/alpha/web-plus/api-crypto/config",
                "/alpha/api/web-plus/api-crypto/config",
                "/beta/web-plus/api-crypto/config",
                "/beta/api/web-plus/api-crypto/config")) {
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
            AtomicBoolean chainCalled = new AtomicBoolean(false);

            filter.filter(exchange, chainExchange -> {
                chainCalled.set(true);
                return Mono.empty();
            }).block();

            assertThat(chainCalled).as("公开配置路径应在登录前放行：%s", path).isTrue();
            assertThat(exchange.getResponse().getStatusCode()).isNull();
        }
    }

    @Test
    void adjacentWebPlusPathIsNotWhitelisted() {
        AiGatewayProperties properties = new AiGatewayProperties();

        assertThat(GatewayPathMatcher.matchesAny(
                properties.getExcludePaths(), "/beta/api/web-plus/internal/config"))
                .isFalse();
    }

    @Test
    void downstreamRouteFailureIsNotHandledAsAuthenticationFailure() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String token = signToken(keyPair, "42");
        GatewayJwtAuthFilter filter = new GatewayJwtAuthFilter(properties(keyPair), redisTemplate(token, "42"));
        MockServerWebExchange exchange = exchange(token);
        ResponseStatusException routeFailure =
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to find service instance");

        assertThatThrownBy(() -> filter.filter(exchange, chainExchange -> Mono.error(routeFailure)).block())
                .isSameAs(routeFailure);
    }

    @Test
    void redisFailureIsHandledAsAuthenticationFailure() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String token = signToken(keyPair, "42");
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        when(redis.hasKey("auth:blacklist:" + token)).thenReturn(Mono.error(new IllegalStateException("redis down")));
        GatewayJwtAuthFilter filter = new GatewayJwtAuthFilter(properties(keyPair), redis);
        MockServerWebExchange exchange = exchange(token);

        filter.filter(exchange, chainExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void configuredSuperAdminTokenUsesDedicatedTokenVersion() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String token = signToken(keyPair, "platform-super-admin", true);
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);
        when(redis.hasKey(AuthConstants.TokenConstants.BLACKLIST_CACHE + ":" + token)).thenReturn(Mono.just(false));
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("platform:auth:super-admin:token-version")).thenReturn(Mono.just("1"));
        GatewayJwtAuthFilter filter = new GatewayJwtAuthFilter(properties(keyPair), redis);
        MockServerWebExchange exchange = exchange(token);
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private static MockServerWebExchange exchange(String token) {
        MockServerHttpRequest request = MockServerHttpRequest.get("/catalog/items")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        return MockServerWebExchange.from(request);
    }

    private static AiGatewayProperties properties(KeyPair keyPair) {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setPublicKey(toPem((RSAPublicKey) keyPair.getPublic()));
        return properties;
    }

    private static ReactiveStringRedisTemplate redisTemplate(String token, String userId) {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveHashOperations<String, Object, Object> hashOperations = mock(ReactiveHashOperations.class);
        when(redis.hasKey(AuthConstants.TokenConstants.BLACKLIST_CACHE + ":" + token)).thenReturn(Mono.just(false));
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(AuthConstants.UserAuthCacheConstants.USER_AUTH_CACHE_PREFIX + userId))
                .thenReturn(reactor.core.publisher.Flux.fromIterable(Map.<Object, Object>of(
                        AuthConstants.UserAuthCacheConstants.FIELD_ENABLED, "true",
                        AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION, "1"
                ).entrySet()));
        return redis;
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String signToken(KeyPair keyPair, String subject) throws Exception {
        return signToken(keyPair, subject, false);
    }

    private static String signToken(KeyPair keyPair, String subject, boolean superAdmin) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim(AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION, 1L)
                .claim("superAdmin", superAdmin)
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
        return jwt.serialize();
    }

    private static String toPem(RSAPublicKey publicKey) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + body + "\n-----END PUBLIC KEY-----";
    }
}
