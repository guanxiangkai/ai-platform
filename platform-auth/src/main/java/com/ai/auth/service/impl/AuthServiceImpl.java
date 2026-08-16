package com.ai.auth.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import com.ai.auth.crypto.RsaJwtServiceImpl;
import com.ai.auth.domain.AuthUserSnapshot;
import com.ai.auth.domain.dto.LoginRequest;
import com.ai.auth.domain.vo.LoginResponse;
import com.ai.auth.log.AuthLoginLogRecord;
import com.ai.auth.properties.AuthSuperAdminProperties;
import com.ai.auth.properties.JwtProperties;
import com.ai.auth.service.AuthProtectionService;
import com.ai.auth.service.IAuthService;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.exception.BaseException;
import io.github.guanxiangkai.web.plus.log.annotation.LoginLog;
import io.github.guanxiangkai.web.plus.core.crypto.SecurityFingerprint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * 认证服务实现类
 * <p>
 * 轻量化设计：直接使用 {@link StringRedisTemplate} 读取认证概要、维护 Token 版本。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
public class AuthServiceImpl implements IAuthService {

    private final PasswordEncoder passwordEncoder;
    private final RsaJwtServiceImpl jwtService;
    private final JwtProperties jwtProperties;
    private final AuthSuperAdminProperties superAdminProperties;
    private final StringRedisTemplate redisTemplate;
    private final AuthProtectionService authProtectionService;

    public AuthServiceImpl(PasswordEncoder passwordEncoder,
                            RsaJwtServiceImpl jwtService,
                            JwtProperties jwtProperties,
                            AuthSuperAdminProperties superAdminProperties,
                             @Lazy @Qualifier("authStringRedisTemplate") StringRedisTemplate redisTemplate,
                             AuthProtectionService authProtectionService) {
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.superAdminProperties = superAdminProperties;
        this.redisTemplate = redisTemplate;
        this.authProtectionService = authProtectionService;
    }

    @Override
    @LoginLog(entity = AuthLoginLogRecord.class, action = "LOGIN")
    public Mono<LoginResponse> login(LoginRequest request, ServerWebExchange exchange) {
        ServerHttpRequest serverRequest = exchange.getRequest();
        return Mono.fromRunnable(() -> authProtectionService.assertLoginAllowed(request.username(), serverRequest))
                .subscribeOn(Schedulers.boundedElastic())
                .then(loadLoginUser(request.username(), request.password()))
                .map(user -> validatePortalTenant(user, serverRequest))
                .flatMap(user -> rotateTokenVersion(user).map(user::withTokenVersion))
                .flatMap(user -> generateTokensAndResponse(user))
                .doOnNext(response -> authProtectionService.recordLoginSuccess(request.username(), serverRequest))
                .doOnError(error -> {
                    if (error instanceof BaseException.BusinessException businessException
                            && businessException.getCode() != 429) {
                        authProtectionService.recordLoginFailure(request.username(), serverRequest);
                    }
                });
    }

    private Mono<AuthUserSnapshot> loadLoginUser(String username, String rawPassword) {
        if (isConfiguredSuperAdminUsername(username)) {
            return loadConfiguredSuperAdmin(rawPassword);
        }
        return loadAuthUserByUsername(username)
                .flatMap(user -> validateUserCredentials(user, rawPassword));
    }

    private AuthUserSnapshot validatePortalTenant(AuthUserSnapshot user, ServerHttpRequest request) {
        if (isConfiguredSuperAdmin(user)) {
            return user;
        }
        String expectedTenantId = request.getHeaders().getFirst(AuthConstants.HeaderConstants.TENANT_ID);
        if (!StringUtils.hasText(expectedTenantId) || !expectedTenantId.equals(user.tenantId())) {
            throw new BaseException.BusinessException("用户名或密码错误");
        }
        return user;
    }

    private Mono<AuthUserSnapshot> loadConfiguredSuperAdmin(String rawPassword) {
        return Mono.fromCallable(() -> {
                    if (!matchesConfiguredSuperAdminPassword(rawPassword)) {
                        throw new BaseException.BusinessException("用户名或密码错误");
                    }
                    return configuredSuperAdminSnapshot(0L);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<AuthUserSnapshot> validateUserCredentials(AuthUserSnapshot user, String rawPassword) {
        return Mono.fromCallable(() -> {
                    if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
                        throw new BaseException.BusinessException("用户名或密码错误");
                    }
                    if (!user.active()) {
                        throw new BaseException.BusinessException("账号已被禁用");
                    }
                    return user;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<LoginResponse> generateTokensAndResponse(AuthUserSnapshot user) {
        return Mono.fromCallable(() -> {
                    Map<String, Object> claims = buildTokenClaims(user);
                    String accessToken = jwtService.generateAccessToken(user.id(), claims);
                    String refreshToken = jwtService.generateRefreshToken(user.id(), claims);
                    storeToken(user.id(), accessToken, refreshToken);
                    return buildLoginResponse(user, accessToken, refreshToken);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> logout(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        return Mono.fromCallable(() -> {
                    try {
                        String token = extractToken(request);
                        if (!StringUtils.hasText(token) || !jwtService.validateAccessToken(token)) {
                            return new LogoutResult(false, null, "退出登录失败：Token 无效或已过期");
                        }

                        String userId = jwtService.getUserIdFromToken(token);
                        if (!StringUtils.hasText(userId) || "anonymousUser".equals(userId)) {
                            return new LogoutResult(false, null, "退出登录失败：用户未认证");
                        }

                        addToBlacklist(token, jwtProperties.getAccessTokenExpirationSeconds());
                        clearToken(userId);
                        return new LogoutResult(true, userId, "退出登录成功");
                    } catch (Exception e) {
                        return new LogoutResult(false, null, "退出登录失败");
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(LogoutResult::success);
    }

    @Override
    @LoginLog(entity = AuthLoginLogRecord.class, action = "REFRESH_TOKEN")
    public Mono<LoginResponse> refreshToken(String refreshToken, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        return Mono.fromCallable(() -> {
                    authProtectionService.assertRefreshAllowed(refreshToken, request);
                    if (!jwtService.validateRefreshToken(refreshToken)) {
                        throw new BaseException.BusinessException("Refresh Token无效或已过期");
                    }
                    if (isInBlacklist(refreshToken)) {
                        throw new BaseException.BusinessException("Refresh Token已失效");
                    }

                    String userId = jwtService.getUserIdFromToken(refreshToken);
                    if (!StringUtils.hasText(userId)) {
                        throw new BaseException.BusinessException("Refresh Token信息不完整");
                    }
                    Long tokenVersion = jwtService.getLongClaimFromToken(refreshToken,
                            AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION);
                    if (tokenVersion == null || tokenVersion <= 0) {
                        throw new BaseException.BusinessException("Refresh Token信息不完整");
                    }

                    String storedRefreshToken = redisTemplate.opsForValue()
                            .get(AuthConstants.TokenConstants.REFRESH_TOKEN_CACHE + ":" + userId);
                    if (!SecurityFingerprint.sha256(refreshToken).equals(storedRefreshToken)) {
                        throw new BaseException.BusinessException("Refresh Token不匹配");
                    }
                    return new RefreshTokenContext(userId, tokenVersion);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(context -> {
                    if (isConfiguredSuperAdminUserId(context.userId())) {
                        return refreshConfiguredSuperAdmin(context, refreshToken, request);
                    }
                    return loadAuthUserById(context.userId())
                            .flatMap(user -> refreshLoadedUser(context, user, refreshToken, request));
                });
    }

    private Mono<LoginResponse> refreshLoadedUser(RefreshTokenContext context,
                                                  AuthUserSnapshot user,
                                                  String refreshToken,
                                                  ServerHttpRequest request) {
        return Mono.fromCallable(() -> {
                    if (!user.active() || user.tokenVersion() != context.tokenVersion()) {
                        throw new BaseException.BusinessException("Refresh Token已失效");
                    }
                    long newTokenVersion = incrementTokenVersion(user.id());
                    AuthUserSnapshot refreshedUser = user.withTokenVersion(newTokenVersion);
                    LoginResponse response = rotateTokensAndBuildResponse(refreshedUser);
                    authProtectionService.recordRefreshSuccess(refreshToken, request);
                    return response;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<LoginResponse> refreshConfiguredSuperAdmin(RefreshTokenContext context,
                                                            String refreshToken,
                                                            ServerHttpRequest request) {
        return Mono.fromCallable(() -> {
                    if (!superAdminProperties.configured()) {
                        throw new BaseException.BusinessException("超级管理员未启用");
                    }
                    long currentTokenVersion = currentConfiguredSuperAdminTokenVersion();
                    if (currentTokenVersion != context.tokenVersion()) {
                        throw new BaseException.BusinessException("Refresh Token已失效");
                    }
                    long newTokenVersion = incrementConfiguredSuperAdminTokenVersion();
                    AuthUserSnapshot refreshedUser = configuredSuperAdminSnapshot(newTokenVersion);
                    LoginResponse response = rotateTokensAndBuildResponse(refreshedUser);
                    authProtectionService.recordRefreshSuccess(refreshToken, request);
                    return response;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private LoginResponse rotateTokensAndBuildResponse(AuthUserSnapshot user) {
        Map<String, Object> claims = buildTokenClaims(user);
        String newAccessToken = jwtService.generateAccessToken(user.id(), claims);
        String newRefreshToken = jwtService.generateRefreshToken(user.id(), claims);
        invalidateCurrentTokens(user.id());
        storeToken(user.id(), newAccessToken, newRefreshToken);
        return buildLoginResponse(user, newAccessToken, newRefreshToken);
    }


    // ==================== 私有辅助方法 ====================

    private Map<String, Object> buildTokenClaims(AuthUserSnapshot user) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("nickname", user.nickname());
        claims.put("superAdmin", user.superAdmin() != null && user.superAdmin());
        claims.put(AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION, user.tokenVersion());

        if (user.tenantId() != null) {
            claims.put("tenantId", user.tenantId());
        }

        if (user.deptId() != null) {
            claims.put("deptId", user.deptId());
        }

        return claims;
    }

    // ==================== Redis 操作 ====================

    private Mono<AuthUserSnapshot> loadAuthUserByUsername(String username) {
        return Mono.fromCallable(() -> {
                    String userId = redisTemplate.opsForValue().get(
                            AuthConstants.UserAuthCacheConstants.USERNAME_INDEX_PREFIX + username);
                    if (!StringUtils.hasText(userId)) {
                        throw new BaseException.BusinessException("用户状态异常，请联系管理员");
                    }
                    return loadAuthUser(userId);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<AuthUserSnapshot> loadAuthUserById(String userId) {
        return Mono.fromCallable(() -> loadAuthUser(userId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private AuthUserSnapshot loadAuthUser(String userId) {
        AuthUserSnapshot user = AuthUserSnapshot.fromHash(redisTemplate.opsForHash().entries(authUserKey(userId)));
        if (user == null) {
            throw new BaseException.BusinessException("用户状态异常，请联系管理员");
        }
        return user.withSuperAdmin(false);
    }

    private boolean isConfiguredSuperAdminUsername(String username) {
        return superAdminProperties.matchesUsername(username);
    }

    private boolean isConfiguredSuperAdminUserId(String userId) {
        return PlatformSuperAdmin.USER_ID.equals(userId);
    }

    private boolean isConfiguredSuperAdmin(AuthUserSnapshot user) {
        return user != null
                && Boolean.TRUE.equals(user.superAdmin())
                && isConfiguredSuperAdminUserId(user.id());
    }

    private AuthUserSnapshot configuredSuperAdminSnapshot(long tokenVersion) {
        return new AuthUserSnapshot(
                PlatformSuperAdmin.USER_ID,
                superAdminProperties.username().trim(),
                superAdminProperties.passwordHash(),
                true,
                tokenVersion,
                "超级管理员",
                null,
                "SUPER_ADMIN",
                true,
                null,
                null,
                Set.of(),
                Set.of(),
                Set.of("*"),
                Set.of()
        );
    }

    private boolean matchesConfiguredSuperAdminPassword(String rawPassword) {
        String configuredPasswordHash = superAdminProperties.passwordHash();
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(configuredPasswordHash)) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, configuredPasswordHash.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Mono<Long> rotateTokenVersion(AuthUserSnapshot user) {
        return Mono.fromCallable(() -> isConfiguredSuperAdmin(user)
                        ? incrementConfiguredSuperAdminTokenVersion()
                        : incrementTokenVersion(user.id()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private long incrementTokenVersion(String userId) {
        Long tokenVersion = redisTemplate.opsForHash().increment(
                authUserKey(userId),
                AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION,
                1
        );
        if (tokenVersion == null || tokenVersion <= 0) {
            throw new BaseException.BusinessException("用户状态异常，请联系管理员");
        }
        return tokenVersion;
    }

    private long incrementConfiguredSuperAdminTokenVersion() {
        Long tokenVersion = redisTemplate.opsForValue().increment(PlatformSuperAdmin.TOKEN_VERSION_CACHE_KEY);
        if (tokenVersion == null || tokenVersion <= 0) {
            throw new BaseException.BusinessException("超级管理员状态异常，请联系管理员");
        }
        return tokenVersion;
    }

    private long currentConfiguredSuperAdminTokenVersion() {
        String value = redisTemplate.opsForValue().get(PlatformSuperAdmin.TOKEN_VERSION_CACHE_KEY);
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void storeToken(String userId, String accessToken, String refreshToken) {
        Duration accessTtl = Duration.ofSeconds(jwtProperties.getAccessTokenExpirationSeconds());
        Duration refreshTtl = Duration.ofSeconds(jwtProperties.getRefreshTokenExpirationSeconds());

        redisTemplate.opsForValue().set(
                AuthConstants.TokenConstants.TOKEN_CACHE + ":" + userId,
                SecurityFingerprint.sha256(accessToken),
                accessTtl);
        redisTemplate.opsForValue().set(
                AuthConstants.TokenConstants.REFRESH_TOKEN_CACHE + ":" + userId,
                SecurityFingerprint.sha256(refreshToken),
                refreshTtl);
    }

    private void clearToken(String userId) {
        redisTemplate.delete(AuthConstants.TokenConstants.TOKEN_CACHE + ":" + userId);
        redisTemplate.delete(AuthConstants.TokenConstants.REFRESH_TOKEN_CACHE + ":" + userId);
    }

    private void addToBlacklist(String token, long ttlSeconds) {
        addFingerprintToBlacklist(SecurityFingerprint.sha256(token), ttlSeconds);
    }

    private void addFingerprintToBlacklist(String fingerprint, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                blacklistKey(fingerprint), "1",
                Duration.ofSeconds(ttlSeconds));
    }

    private boolean isInBlacklist(String token) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(blacklistKey(SecurityFingerprint.sha256(token))));
    }

    private String blacklistKey(String fingerprint) {
        return AuthConstants.TokenConstants.BLACKLIST_CACHE + ":" + fingerprint;
    }

    private String authUserKey(String userId) {
        return AuthConstants.UserAuthCacheConstants.USER_AUTH_CACHE_PREFIX + userId;
    }

    private void invalidateCurrentTokens(String userId) {
        String oldAccessTokenFingerprint = redisTemplate.opsForValue()
                .get(AuthConstants.TokenConstants.TOKEN_CACHE + ":" + userId);
        if (StringUtils.hasText(oldAccessTokenFingerprint)) {
            addFingerprintToBlacklist(
                    oldAccessTokenFingerprint, jwtProperties.getAccessTokenExpirationSeconds());
        }

        String oldRefreshTokenFingerprint = redisTemplate.opsForValue()
                .get(AuthConstants.TokenConstants.REFRESH_TOKEN_CACHE + ":" + userId);
        if (StringUtils.hasText(oldRefreshTokenFingerprint)) {
            addFingerprintToBlacklist(
                    oldRefreshTokenFingerprint, jwtProperties.getRefreshTokenExpirationSeconds());
        }
    }

    // ==================== Token / 响应构建 ====================

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private LoginResponse buildLoginResponse(AuthUserSnapshot user, String accessToken, String refreshToken) {
        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.getAccessTokenExpirationSeconds(),
                user.id(),
                user.username(),
                user.nickname(),
                user.avatar(),
                user.userType(),
                user.superAdmin(),
                user.superAdmin() ? null : user.roleCodes(),
                user.superAdmin() ? null : user.postCodes(),
                user.permissions(),
                user.deptId(),
                user.deptIds()
        );
    }

    private record LogoutResult(boolean success, String userId, String message) {
    }

    private record RefreshTokenContext(String userId, long tokenVersion) {
    }
}
