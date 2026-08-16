package com.ai.gateway.filter;

import com.ai.api.security.PlatformSuperAdmin;
import com.ai.api.security.PlatformTokenProfile;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.crypto.SecurityFingerprint;
import com.ai.gateway.config.AiGatewayProperties;
import com.ai.gateway.util.ReactiveResponseUtils;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;

/**
 * 网关 JWT 认证过滤器（RSA 公钥本地验证）
 * <p>
 * 从 Nacos 配置加载 RSA 公钥，本地验证 JWT 签名和有效期，
 * 同时检查 Redis 黑名单与用户认证概要中的 tokenVersion / 状态。
 * <br/>
 * ⚠️ 不调用 Auth 服务任何接口，完全本地验证。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
public class GatewayJwtAuthFilter implements WebFilter {

    private static final int MINIMUM_RSA_BITS = 2_048;

    private final AiGatewayProperties props;
    private final ReactiveStringRedisTemplate authRedisTemplate;
    private volatile String verifierPem;
    private volatile RSASSAVerifier cachedVerifier;

    public GatewayJwtAuthFilter(AiGatewayProperties props,
                                ReactiveStringRedisTemplate authRedisTemplate) {
        this.props = props;
        this.authRedisTemplate = authRedisTemplate;
        verifier();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String rawAuthHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.debug("[GatewayAuth] filter entered: path={}, hasAuthHeader={}", path, rawAuthHeader != null);

        // 1. 白名单放行
        if (shouldSkip(path)) {
            log.debug("[GatewayAuth] path whitelisted, skipping: {}", path);
            return chain.filter(exchange);
        }

        // 2. 提取 Token（大小写不敏感 Bearer 前缀）
        String token = extractToken(exchange.getRequest());
        if (token == null) {
            // Authorization 头属于凭据；格式错误时也只能记录元数据，绝不能输出内容片段。
            if (rawAuthHeader != null) {
                log.warn("[GatewayAuth] Authorization 头存在但不是有效 Bearer 格式: path={}, headerLength={}",
                        path, rawAuthHeader.length());
            } else {
                log.debug("[GatewayAuth] 无 Authorization 头，交由 Spring Security 处理: path={}", path);
            }
            return chain.filter(exchange);
        }
        // 3. 验证签名 + 有效期
        JWTClaimsSet claims;
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!isExpectedAccessTokenHeader(jwt)) {
                log.warn("[GatewayAuth] JWT 算法、类型或 keyId 不符合访问令牌配置: path={}", path);
                return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "Token 类型无效");
            }
            if (!jwt.verify(verifier())) {
                log.warn("[GatewayAuth] JWT 签名验证失败（公私钥不匹配？）: path={}", path);
                return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "Token 签名无效");
            }
            claims = jwt.getJWTClaimsSet();
            if (!isExpectedAccessTokenClaims(claims)) {
                log.warn("[GatewayAuth] JWT 声明不符合访问令牌配置: path={}", path);
                return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "Token 声明无效");
            }
        } catch (Exception e) {
            log.warn("[GatewayAuth] JWT 解析/验证异常: path={}, exception={}",
                    path, e.getClass().getSimpleName());
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "Token 格式无效");
        }

        String userId = claims.getSubject();
        if (!StringUtils.hasText(userId)) {
            log.warn("[GatewayAuth] JWT 中缺少 sub（userId）: path={}", path);
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "Token 格式无效");
        }

        Long tokenVersion = readTokenVersion(claims);
        if (tokenVersion == null || tokenVersion <= 0) {
            log.warn("[GatewayAuth] JWT 中缺少 tokenVersion: path={}", path);
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "Token 格式无效");
        }

        // 4. Redis 黑名单 + 用户认证概要状态
        boolean superAdmin = Boolean.parseBoolean(String.valueOf(claims.getClaim("superAdmin")));
        return checkTokenActive(token, userId, tokenVersion, superAdmin)
                .onErrorResume(e -> {
                    log.error("[GatewayAuth] Token 活跃状态校验异常: path={}, exception={}",
                            path, e.getClass().getSimpleName());
                    return Mono.just(false);
                })
                .flatMap(active -> {
                    if (!active) {
                        log.warn("[GatewayAuth] Token 不活跃（已登出/版本不一致/用户被禁用）: path={}", path);
                        return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
                    }

                    // 5. 写入 SecurityContext（使用 Spring Security 7 推荐 API）
                    Map<String, Object> claimsMap = new HashMap<>(claims.getClaims());
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    auth.setDetails(claimsMap);
                    log.debug("[GatewayAuth] 认证通过，写入 SecurityContext: path={}", path);
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                });
    }

    // ==================== 私有方法 ====================

    private boolean isExpectedAccessTokenHeader(SignedJWT jwt) {
        return JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())
                && jwt.getHeader().getType() != null
                && PlatformTokenProfile.ACCESS_TOKEN_TYPE.equals(jwt.getHeader().getType().toString())
                && props.getKeyId().equals(jwt.getHeader().getKeyID());
    }

    private boolean isExpectedAccessTokenClaims(JWTClaimsSet claims) throws java.text.ParseException {
        Instant now = Instant.now();
        long skewSeconds = props.getAllowedClockSkew().toSeconds();
        Date expiration = claims.getExpirationTime();
        Date issueTime = claims.getIssueTime();
        Date notBeforeTime = claims.getNotBeforeTime();
        return expiration != null
                && expiration.toInstant().isAfter(now.minusSeconds(skewSeconds))
                && issueTime != null
                && !issueTime.toInstant().isAfter(now.plusSeconds(skewSeconds))
                && expiration.toInstant().isAfter(issueTime.toInstant())
                && (notBeforeTime == null
                    || (!notBeforeTime.toInstant().isAfter(now.plusSeconds(skewSeconds))
                        && !notBeforeTime.toInstant().isAfter(expiration.toInstant())))
                && claims.getJWTID() != null
                && !claims.getJWTID().isBlank()
                && props.getIssuer().equals(claims.getIssuer())
                && List.of(props.getAccessTokenAudience()).equals(claims.getAudience())
                && PlatformTokenProfile.ACCESS_TOKEN_USE.equals(
                claims.getStringClaim(PlatformTokenProfile.TOKEN_USE_CLAIM));
    }

    private RSASSAVerifier verifier() {
        String publicKeyPem = props.getPublicKey();
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw new IllegalStateException("未配置 ai.gateway.public-key，Gateway 服务拒绝启动");
        }
        RSASSAVerifier current = cachedVerifier;
        if (current != null && publicKeyPem.equals(verifierPem)) {
            return current;
        }
        synchronized (this) {
            current = cachedVerifier;
            if (current != null && publicKeyPem.equals(verifierPem)) {
                return current;
            }
            RSASSAVerifier parsed = parseVerifier(publicKeyPem);
            verifierPem = publicKeyPem;
            cachedVerifier = parsed;
            return parsed;
        }
    }

    private RSASSAVerifier parseVerifier(String publicKeyPem) {
        try {
            String cleaned = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            RSAPublicKey rsaPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
            if (rsaPublicKey.getModulus().bitLength() < MINIMUM_RSA_BITS) {
                throw new IllegalStateException("RSA 公钥强度不能低于 " + MINIMUM_RSA_BITS + " 位");
            }
            return new RSASSAVerifier(rsaPublicKey);
        } catch (Exception e) {
            if (e instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException("加载 RSA 公钥失败", e);
        }
    }

    private Mono<Boolean> checkTokenActive(String token, String userId, long tokenVersion, boolean superAdmin) {
        return authRedisTemplate.hasKey(
                        AuthConstants.TokenConstants.BLACKLIST_CACHE + ":" + SecurityFingerprint.sha256(token))
                .flatMap(inBlacklist -> {
                    if (inBlacklist) {
                        log.warn("[GatewayAuth] Token 已在黑名单（已登出）");
                        return Mono.just(false);
                    }
                    if (superAdmin) {
                        return checkConfiguredSuperAdminTokenActive(userId, tokenVersion);
                    }
                    return authRedisTemplate.opsForHash()
                            .entries(AuthConstants.UserAuthCacheConstants.USER_AUTH_CACHE_PREFIX + userId)
                            .collectMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString())
                            .map(authData -> isAuthDataActive(authData, tokenVersion))
                            .switchIfEmpty(Mono.fromCallable(() -> {
                                log.warn("[GatewayAuth] Redis 中不存在用户认证概要");
                                return false;
                            }));
                });
    }

    private Mono<Boolean> checkConfiguredSuperAdminTokenActive(String userId, long tokenVersion) {
        if (!PlatformSuperAdmin.USER_ID.equals(userId)) {
            log.warn("[GatewayAuth] 超级管理员 Token subject 非平台超级管理员虚拟账号");
            return Mono.just(false);
        }
        return authRedisTemplate.opsForValue()
                .get(PlatformSuperAdmin.TOKEN_VERSION_CACHE_KEY)
                .map(currentVersion -> {
                    boolean active = String.valueOf(tokenVersion).equals(currentVersion);
                    if (!active) {
                        log.warn("[GatewayAuth] 超级管理员 Token 版本校验失败");
                    }
                    return active;
                })
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.warn("[GatewayAuth] Redis 中不存在超级管理员 tokenVersion");
                    return false;
                }));
    }

    private boolean isAuthDataActive(Map<String, String> authData, long tokenVersion) {
        String enabled = authData.get(AuthConstants.UserAuthCacheConstants.FIELD_ENABLED);
        String currentVersion = authData.get(AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION);
        boolean active = Boolean.parseBoolean(enabled)
                && String.valueOf(tokenVersion).equals(currentVersion);
        if (!active) {
            log.warn("[GatewayAuth] 用户认证概要校验失败: enabled={}", enabled);
        }
        return active;
    }

    private Long readTokenVersion(JWTClaimsSet claims) {
        Object value = claims.getClaim(AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean shouldSkip(String path) {
        return props.isExcludedPath(path);
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.length() > 7
                && bearerToken.substring(0, 7).equalsIgnoreCase("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
