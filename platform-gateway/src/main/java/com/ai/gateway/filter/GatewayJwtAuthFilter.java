package com.ai.gateway.filter;

import com.ai.api.security.PlatformSuperAdmin;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import com.ai.gateway.config.AiGatewayProperties;
import com.ai.gateway.util.GatewayPathMatcher;
import com.ai.gateway.util.ReactiveResponseUtils;
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

    private final AiGatewayProperties props;
    private final ReactiveStringRedisTemplate authRedisTemplate;
    private final RSASSAVerifier verifier;

    public GatewayJwtAuthFilter(AiGatewayProperties props,
                                ReactiveStringRedisTemplate authRedisTemplate) {
        this.props = props;
        this.authRedisTemplate = authRedisTemplate;
        this.verifier = initVerifier(props.getPublicKey());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String rawAuthHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.info("[GatewayAuth] >>> filter entered: path={}, hasAuthHeader={}", path, rawAuthHeader != null);

        // 1. 白名单放行
        if (shouldSkip(path)) {
            log.debug("[GatewayAuth] path whitelisted, skipping: {}", path);
            return chain.filter(exchange);
        }

        // 2. 提取 Token（大小写不敏感 Bearer 前缀）
        String token = extractToken(exchange.getRequest());
        if (token == null) {
            // Authorization 头存在但格式不对，记录实际值帮助诊断
            if (rawAuthHeader != null) {
                String preview = rawAuthHeader.length() > 30 ? rawAuthHeader.substring(0, 30) + "..." : rawAuthHeader;
                log.warn("[GatewayAuth] Authorization 头存在但不是有效 Bearer 格式: path={}, headerPreview='{}'", path, preview);
            } else {
                log.debug("[GatewayAuth] 无 Authorization 头，交由 Spring Security 处理: path={}", path);
            }
            return chain.filter(exchange);
        }
        // verifier 未初始化（RSA 公钥未配置）时快速失败，拒绝访问
        if (verifier == null) {
            log.warn("[GatewayAuth] JWT verifier 未初始化（ai.gateway.public-key 未配置），拒绝: {}", path);
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "网关认证未配置，请联系管理员");
        }

        // 3. 验证签名 + 有效期
        JWTClaimsSet claims;
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(verifier)) {
                log.warn("[GatewayAuth] JWT 签名验证失败（公私钥不匹配？）: path={}", path);
                return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "Token 签名无效");
            }
            claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
                log.warn("[GatewayAuth] JWT 已过期: path={}, exp={}", path, claims.getExpirationTime());
                return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
            }
        } catch (Exception e) {
            log.warn("[GatewayAuth] JWT 解析/验证异常: path={}, error={}", path, e.getMessage());
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "Token 格式无效");
        }

        String userId = claims.getSubject();
        if (!StringUtils.hasText(userId)) {
            log.warn("[GatewayAuth] JWT 中缺少 sub（userId）: path={}", path);
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "Token 格式无效");
        }

        Long tokenVersion = readTokenVersion(claims);
        if (tokenVersion == null || tokenVersion <= 0) {
            log.warn("[GatewayAuth] JWT 中缺少 tokenVersion: path={}, userId={}", path, userId);
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "Token 格式无效");
        }

        // 4. Redis 黑名单 + 用户认证概要状态
        boolean superAdmin = Boolean.parseBoolean(String.valueOf(claims.getClaim("superAdmin")));
        return checkTokenActive(token, userId, tokenVersion, superAdmin)
                .onErrorResume(e -> {
                    log.error("[GatewayAuth] Token 活跃状态校验异常: path={}, userId={}, error={}", path, userId, e.getMessage(), e);
                    return Mono.just(false);
                })
                .flatMap(active -> {
                    if (!active) {
                        log.warn("[GatewayAuth] Token 不活跃（已登出/版本不一致/用户被禁用）: path={}, userId={}", path, userId);
                        return ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
                    }

                    // 5. 写入 SecurityContext（使用 Spring Security 7 推荐 API）
                    Map<String, Object> claimsMap = new HashMap<>(claims.getClaims());
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    auth.setDetails(claimsMap);
                    log.info("[GatewayAuth] 认证通过，写入 SecurityContext: path={}, userId={}", path, userId);
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                });
    }

    // ==================== 私有方法 ====================

    private RSASSAVerifier initVerifier(String publicKeyPem) {
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            log.warn("未配置 RSA 公钥（ai.gateway.public-key），JWT 验证将不可用");
            return null;
        }
        try {
            String cleaned = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            RSAPublicKey rsaPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
            return new RSASSAVerifier(rsaPublicKey);
        } catch (Exception e) {
            throw new IllegalStateException("加载 RSA 公钥失败", e);
        }
    }

    private Mono<Boolean> checkTokenActive(String token, String userId, long tokenVersion, boolean superAdmin) {
        return authRedisTemplate.hasKey(AuthConstants.TokenConstants.BLACKLIST_CACHE + ":" + token)
                .flatMap(inBlacklist -> {
                    if (inBlacklist) {
                        log.warn("[GatewayAuth] Token 已在黑名单（已登出）: userId={}", userId);
                        return Mono.just(false);
                    }
                    if (superAdmin) {
                        return checkConfiguredSuperAdminTokenActive(userId, tokenVersion);
                    }
                    return authRedisTemplate.opsForHash()
                            .entries(AuthConstants.UserAuthCacheConstants.USER_AUTH_CACHE_PREFIX + userId)
                            .collectMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString())
                            .map(authData -> isAuthDataActive(authData, tokenVersion, userId))
                            .switchIfEmpty(Mono.fromCallable(() -> {
                                log.warn("[GatewayAuth] Redis 中不存在用户认证概要: key={}", AuthConstants.UserAuthCacheConstants.USER_AUTH_CACHE_PREFIX + userId);
                                return false;
                            }));
                });
    }

    private Mono<Boolean> checkConfiguredSuperAdminTokenActive(String userId, long tokenVersion) {
        if (!PlatformSuperAdmin.USER_ID.equals(userId)) {
            log.warn("[GatewayAuth] 超级管理员 Token subject 非平台超级管理员虚拟账号: userId={}", userId);
            return Mono.just(false);
        }
        return authRedisTemplate.opsForValue()
                .get(PlatformSuperAdmin.TOKEN_VERSION_CACHE_KEY)
                .map(currentVersion -> {
                    boolean active = String.valueOf(tokenVersion).equals(currentVersion);
                    if (!active) {
                        log.warn("[GatewayAuth] 超级管理员 Token 版本校验失败: tokenVersion={}, currentVersion={}",
                                tokenVersion, currentVersion);
                    }
                    return active;
                })
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.warn("[GatewayAuth] Redis 中不存在超级管理员 tokenVersion: key={}",
                            PlatformSuperAdmin.TOKEN_VERSION_CACHE_KEY);
                    return false;
                }));
    }

    private boolean isAuthDataActive(Map<String, String> authData, long tokenVersion, String userId) {
        String enabled = authData.get(AuthConstants.UserAuthCacheConstants.FIELD_ENABLED);
        String currentVersion = authData.get(AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION);
        boolean active = Boolean.parseBoolean(enabled)
                && String.valueOf(tokenVersion).equals(currentVersion);
        if (!active) {
            log.warn("[GatewayAuth] 用户认证概要校验失败: userId={}, enabled={}, tokenVersion={}, currentVersion={}",
                    userId, enabled, tokenVersion, currentVersion);
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
        return GatewayPathMatcher.matchesAny(props.getExcludePaths(), path);
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
