package com.ai.auth.crypto;

import lombok.extern.slf4j.Slf4j;

import com.ai.auth.properties.JwtProperties;
import io.github.guanxiangkai.web.plus.security.service.IJwtService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * RSA JWT 服务实现（Security 模块专用，RS256 非对称签名）
 * <p>
 * 使用 {@link RsaKeyPairManager} 的 RSA 私钥签名 JWT。
 * {@code @Primary} 覆盖 {@code web-plus-security} 中注册的 HMAC 默认实现。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Primary
@Component
@RequiredArgsConstructor
@Slf4j
public class RsaJwtServiceImpl implements IJwtService {

    private final JwtProperties properties;
    private final RsaKeyPairManager keyManager;
    private JWSSigner signer;
    private JWSVerifier verifier;

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
     * 生成访问令牌（RS256 签名）
     */
    public String generateAccessToken(String userId, Map<String, Object> claims) {
        return generateToken(userId, claims, properties.getAccessTokenExpirationSeconds());
    }

    /**
     * 生成带业务声明的刷新令牌。
     */
    public String generateRefreshToken(String userId, Map<String, Object> claims) {
        return generateToken(userId, claims, properties.getRefreshTokenExpirationSeconds());
    }

    /**
     * 验证令牌签名 + 有效期（Auth 内部使用，如 logout/refresh 时需解析 token）
     */
    public boolean validateToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(verifier)) {
                return false;
            }
            Date expiration = jwt.getJWTClaimsSet().getExpirationTime();
            return expiration != null && expiration.after(new Date());
        } catch (Exception e) {
            log.debug("RSA 令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getUserIdFromToken(String token) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            log.warn("解析令牌 subject 失败: {}", e.getMessage());
            return null;
        }
    }

    public Long getLongClaimFromToken(String token, String claimName) {
        try {
            Object value = SignedJWT.parse(token).getJWTClaimsSet().getClaim(claimName);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                return Long.parseLong(text);
            }
            return null;
        } catch (Exception e) {
            log.warn("解析令牌数值声明失败: claim={}, error={}", claimName, e.getMessage());
            return null;
        }
    }

    private String generateToken(String userId, Map<String, Object> claims, long expirationSeconds) {
        try {
            Instant now = Instant.now();

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(userId)
                    .issuer(properties.issuer())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(expirationSeconds)));

            for (Map.Entry<String, Object> entry : claims.entrySet()) {
                builder.claim(entry.getKey(), entry.getValue());
            }

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(keyManager.getKeyId())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, builder.build());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("RSA 签名 JWT 失败", e);
        }
    }
}
