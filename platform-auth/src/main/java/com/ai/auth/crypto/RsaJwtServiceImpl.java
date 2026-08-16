package com.ai.auth.crypto;

import com.ai.auth.properties.JwtProperties;
import com.ai.api.security.PlatformTokenProfile;
import io.github.guanxiangkai.web.plus.security.service.IJwtService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * RSA JWT 服务实现（Security 模块专用，RS256 非对称签名）
 * <p>
 * 使用 {@link RsaKeyPairManager} 的 RSA 私钥签名 JWT，并执行平台专属令牌概要验证。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RsaJwtServiceImpl implements IJwtService {

    private static final Set<String> RESERVED_CLAIMS = Set.of(
            "iss", "sub", "aud", "exp", "nbf", "iat", "jti",
            PlatformTokenProfile.TOKEN_USE_CLAIM
    );

    private final JwtProperties properties;
    private final RsaKeyPairManager keyManager;
    private JWSSigner signer;
    private JWSVerifier verifier;

    /**
     * 使用当前受控 RSA 密钥初始化线程安全的签名器和验证器。
     */
    @PostConstruct
    public void init() {
        try {
            this.signer = new RSASSASigner(keyManager.getPrivateKey());
            this.verifier = new RSASSAVerifier(keyManager.getPublicKey());
            log.info("RSA JWT 服务初始化完成（RS256），keyId={}", keyManager.getKeyId());
        } catch (Exception e) {
            throw new IllegalStateException("初始化 RSA JWT 签名器失败", e);
        }
    }

    /**
     * 生成访问令牌（RS256 签名）。
     *
     * @param userId JWT subject
     * @param claims 平台业务声明；标准安全声明由本服务覆盖
     * @return 已签名访问令牌
     */
    public String generateAccessToken(String userId, Map<String, Object> claims) {
        requireUserId(userId);
        return generateToken(
                userId,
                claims,
                properties.getAccessTokenExpirationSeconds(),
                PlatformTokenProfile.ACCESS_TOKEN_USE,
                PlatformTokenProfile.ACCESS_TOKEN_TYPE,
                properties.accessTokenAudience()
        );
    }

    /**
     * 生成带业务声明的刷新令牌。
     *
     * @param userId JWT subject
     * @param claims 平台业务声明；标准安全声明由本服务覆盖
     * @return 已签名刷新令牌
     */
    public String generateRefreshToken(String userId, Map<String, Object> claims) {
        requireUserId(userId);
        return generateToken(
                userId,
                claims,
                properties.getRefreshTokenExpirationSeconds(),
                PlatformTokenProfile.REFRESH_TOKEN_USE,
                PlatformTokenProfile.REFRESH_TOKEN_TYPE,
                properties.refreshTokenAudience()
        );
    }

    /**
     * 验证访问令牌的签名、算法、类型、签发方、受众和时间声明。
     *
     * @param token 待验证令牌
     * @return 全部访问令牌约束均满足时返回 {@code true}
     */
    public boolean validateAccessToken(String token) {
        return validatedClaims(
                token,
                PlatformTokenProfile.ACCESS_TOKEN_USE,
                PlatformTokenProfile.ACCESS_TOKEN_TYPE,
                properties.accessTokenAudience()
        ).isPresent();
    }

    /**
     * 使用与访问令牌互斥的规则验证刷新令牌。
     *
     * @param token 待验证令牌
     * @return 全部刷新令牌约束均满足时返回 {@code true}
     */
    public boolean validateRefreshToken(String token) {
        return validatedClaims(
                token,
                PlatformTokenProfile.REFRESH_TOKEN_USE,
                PlatformTokenProfile.REFRESH_TOKEN_TYPE,
                properties.refreshTokenAudience()
        ).isPresent();
    }

    private Optional<JWTClaimsSet> validatedClaims(String token,
                                                   String expectedUse,
                                                   String expectedType,
                                                   String expectedAudience) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWSHeader header = jwt.getHeader();
            if (!JWSAlgorithm.RS256.equals(header.getAlgorithm())
                    || header.getType() == null
                    || !expectedType.equals(header.getType().toString())
                    || !Objects.equals(keyManager.getKeyId(), header.getKeyID())) {
                return Optional.empty();
            }
            if (!jwt.verify(verifier)) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant now = Instant.now();
            long skewSeconds = properties.allowedClockSkew().toSeconds();
            Date expiration = claims.getExpirationTime();
            Date issueTime = claims.getIssueTime();
            Date notBeforeTime = claims.getNotBeforeTime();
            boolean valid = expiration != null
                    && expiration.toInstant().isAfter(now.minusSeconds(skewSeconds))
                    && issueTime != null
                    && !issueTime.toInstant().isAfter(now.plusSeconds(skewSeconds))
                    && expiration.toInstant().isAfter(issueTime.toInstant())
                    && (notBeforeTime == null
                        || (!notBeforeTime.toInstant().isAfter(now.plusSeconds(skewSeconds))
                            && !notBeforeTime.toInstant().isAfter(expiration.toInstant())))
                    && claims.getSubject() != null
                    && !claims.getSubject().isBlank()
                    && properties.issuer().equals(claims.getIssuer())
                    && List.of(expectedAudience).equals(claims.getAudience())
                    && expectedUse.equals(claims.getStringClaim(PlatformTokenProfile.TOKEN_USE_CLAIM))
                    && claims.getJWTID() != null
                    && !claims.getJWTID().isBlank();
            return valid ? Optional.of(claims) : Optional.empty();
        } catch (Exception e) {
            log.debug("RSA 令牌验证失败，异常类型={}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * 从经过完整平台 JWT 概要验证的令牌中读取 subject。
     *
     * <p>访问令牌和刷新令牌都可读取；未知用途、签名无效或声明不完整时不返回身份。</p>
     *
     * @param token 待验证令牌
     * @return 已验证 subject；无效令牌返回 {@code null}
     */
    @Override
    public String getUserIdFromToken(String token) {
        return validatedKnownProfileClaims(token)
                .map(JWTClaimsSet::getSubject)
                .orElse(null);
    }

    /**
     * 从经过完整平台 JWT 概要验证的令牌中读取长整型声明。
     *
     * @param token     待验证令牌
     * @param claimName 声明名称
     * @return 已验证数值声明；令牌或声明无效时返回 {@code null}
     */
    public Long getLongClaimFromToken(String token, String claimName) {
        try {
            Object value = validatedKnownProfileClaims(token)
                    .map(claims -> claims.getClaim(claimName))
                    .orElse(null);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                return Long.parseLong(text);
            }
            return null;
        } catch (Exception e) {
            log.debug("读取已验证令牌数值声明失败: claim={}, exception={}",
                    claimName, e.getClass().getSimpleName());
            return null;
        }
    }

    private Optional<JWTClaimsSet> validatedKnownProfileClaims(String token) {
        Optional<JWTClaimsSet> accessClaims = validatedClaims(
                token,
                PlatformTokenProfile.ACCESS_TOKEN_USE,
                PlatformTokenProfile.ACCESS_TOKEN_TYPE,
                properties.accessTokenAudience()
        );
        if (accessClaims.isPresent()) {
            return accessClaims;
        }
        return validatedClaims(
                token,
                PlatformTokenProfile.REFRESH_TOKEN_USE,
                PlatformTokenProfile.REFRESH_TOKEN_TYPE,
                properties.refreshTokenAudience()
        );
    }

    private String generateToken(String userId,
                                 Map<String, Object> claims,
                                 long expirationSeconds,
                                 String tokenUse,
                                 String tokenType,
                                 String audience) {
        try {
            Instant now = Instant.now();

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
            if (claims != null) {
                for (Map.Entry<String, Object> entry : claims.entrySet()) {
                    String claimName = entry.getKey();
                    if (claimName == null || claimName.isBlank()) {
                        throw new IllegalArgumentException("JWT 业务声明名称不能为空");
                    }
                    if (!RESERVED_CLAIMS.contains(claimName)) {
                        builder.claim(claimName, entry.getValue());
                    }
                }
            }
            builder
                    .subject(userId)
                    .issuer(properties.issuer())
                    .audience(audience)
                    .jwtID(UUID.randomUUID().toString())
                    .claim(PlatformTokenProfile.TOKEN_USE_CLAIM, tokenUse)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(expirationSeconds)));

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(new JOSEObjectType(tokenType))
                    .keyID(keyManager.getKeyId())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, builder.build());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("RSA 签名 JWT 失败", e);
        }
    }

    private static void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("JWT subject 不能为空");
        }
    }
}
