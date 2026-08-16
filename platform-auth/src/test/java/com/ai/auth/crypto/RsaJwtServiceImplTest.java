package com.ai.auth.crypto;

import com.ai.auth.properties.JwtProperties;
import com.ai.api.security.PlatformTokenProfile;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RsaJwtServiceImplTest {

    private RsaJwtServiceImpl jwtService;
    private RSAPrivateKey signingKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        signingKey = (RSAPrivateKey) keyPair.getPrivate();

        JwtProperties properties = new JwtProperties(
                null,
                "test-key",
                Duration.ofMinutes(5),
                Duration.ofHours(1),
                "test-issuer",
                "test-gateway",
                "test-auth",
                Duration.ofSeconds(60)
        );
        RsaKeyPairManager keyManager = mock(RsaKeyPairManager.class);
        when(keyManager.getPrivateKey()).thenReturn(signingKey);
        when(keyManager.getPublicKey()).thenReturn((RSAPublicKey) keyPair.getPublic());
        when(keyManager.getKeyId()).thenReturn("test-key");

        jwtService = new RsaJwtServiceImpl(properties, keyManager);
        jwtService.init();
    }

    @Test
    void accessAndRefreshTokensUseMutuallyExclusiveProfiles() throws Exception {
        String accessToken = jwtService.generateAccessToken("42", Map.of("tokenVersion", 1L));
        String refreshToken = jwtService.generateRefreshToken("42", Map.of("tokenVersion", 1L));

        assertThat(jwtService.validateAccessToken(accessToken)).isTrue();
        assertThat(jwtService.validateRefreshToken(accessToken)).isFalse();
        assertThat(jwtService.validateRefreshToken(refreshToken)).isTrue();
        assertThat(jwtService.validateAccessToken(refreshToken)).isFalse();
        assertThat(jwtService.getUserIdFromToken(accessToken)).isEqualTo("42");
        assertThat(jwtService.getUserIdFromToken(refreshToken)).isEqualTo("42");
        assertThat(jwtService.getLongClaimFromToken(refreshToken, "tokenVersion")).isEqualTo(1L);

        SignedJWT accessJwt = SignedJWT.parse(accessToken);
        assertThat(accessJwt.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(accessJwt.getHeader().getType().toString())
                .isEqualTo(PlatformTokenProfile.ACCESS_TOKEN_TYPE);
        assertThat(accessJwt.getJWTClaimsSet().getIssuer()).isEqualTo("test-issuer");
        assertThat(accessJwt.getJWTClaimsSet().getAudience()).containsExactly("test-gateway");
        assertThat(accessJwt.getJWTClaimsSet().getStringClaim(PlatformTokenProfile.TOKEN_USE_CLAIM))
                .isEqualTo(PlatformTokenProfile.ACCESS_TOKEN_USE);
        assertThat(accessJwt.getJWTClaimsSet().getJWTID()).isNotBlank();
    }

    @Test
    void callerCannotOverrideRegisteredSecurityClaims() throws Exception {
        String token = jwtService.generateAccessToken("42", Map.of(
                "iss", "attacker",
                "aud", "attacker",
                "sub", "attacker",
                "nbf", Date.from(Instant.now().plus(Duration.ofHours(1))),
                PlatformTokenProfile.TOKEN_USE_CLAIM, PlatformTokenProfile.REFRESH_TOKEN_USE
        ));

        assertThat(jwtService.validateAccessToken(token)).isTrue();
        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("test-issuer");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("test-gateway");
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("42");
        assertThat(jwt.getJWTClaimsSet().getNotBeforeTime()).isNull();
        assertThat(jwt.getJWTClaimsSet().getStringClaim(PlatformTokenProfile.TOKEN_USE_CLAIM))
                .isEqualTo(PlatformTokenProfile.ACCESS_TOKEN_USE);
    }

    @Test
    void tokenWithNotBeforeAfterExpirationIsRejected() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("42")
                .issuer("test-issuer")
                .audience("test-gateway")
                .jwtID("invalid-window-jti")
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.plusSeconds(600)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim(PlatformTokenProfile.TOKEN_USE_CLAIM, PlatformTokenProfile.ACCESS_TOKEN_USE)
                .claim("tokenVersion", 1L)
                .build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(new JOSEObjectType(PlatformTokenProfile.ACCESS_TOKEN_TYPE))
                .keyID("test-key")
                .build();
        SignedJWT signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(new RSASSASigner(signingKey));

        assertThat(jwtService.validateAccessToken(signedJwt.serialize())).isFalse();
        assertThat(jwtService.getUserIdFromToken(signedJwt.serialize())).isNull();
    }

    @Test
    void unverifiedTokenCannotExposeIdentityOrClaims() {
        String validToken = jwtService.generateAccessToken("42", Map.of("tokenVersion", 1L));
        int signatureStart = validToken.lastIndexOf('.') + 1;
        char original = validToken.charAt(signatureStart);
        char replacement = original == 'A' ? 'B' : 'A';
        String tamperedToken = validToken.substring(0, signatureStart)
                + replacement
                + validToken.substring(signatureStart + 1);

        assertThat(jwtService.validateAccessToken(tamperedToken)).isFalse();
        assertThat(jwtService.getUserIdFromToken(tamperedToken)).isNull();
        assertThat(jwtService.getLongClaimFromToken(tamperedToken, "tokenVersion")).isNull();
    }
}
