package com.ai.auth.crypto;

import lombok.extern.slf4j.Slf4j;

import com.ai.auth.properties.JwtProperties;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * RSA 密钥对管理器（Security 模块专用）
 * <p>
 * 从运行配置加载 RSA 私钥（或开发环境自动生成），
 * 为 JWT 签名服务提供签名私钥。
 * <br/>
 * ⚠️ 私钥仅在 Auth 服务内部使用，绝不对外暴露。
 * 公钥由部署环境同步提供给 Gateway。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
@Slf4j
public class RsaKeyPairManager {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;

    public RsaKeyPairManager(JwtProperties properties) {
        this.keyId = properties.keyId();

        if (properties.privateKey() != null && !properties.privateKey().isBlank()) {
            log.info("从配置加载 RSA 私钥, keyId={}", keyId);
            this.privateKey = loadPrivateKey(properties.privateKey());
            this.publicKey = derivePublicKey(this.privateKey);
        } else {
            log.warn("未配置 RSA 私钥，自动生成 2048 位密钥对（仅限开发环境！）");
            KeyPair keyPair = generateKeyPair();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
        }

        log.info("RSA 密钥对初始化完成, keyId={}", keyId);
    }

    // ==================== 私有方法 ====================

    private static RSAPrivateKey loadPrivateKey(String pem) {
        try {
            String cleaned = pem
                    .replace(pemBoundary("BEGIN", "PRIVATE KEY"), "")
                    .replace(pemBoundary("END", "PRIVATE KEY"), "")
                    .replace(pemBoundary("BEGIN", "RSA PRIVATE KEY"), "")
                    .replace(pemBoundary("END", "RSA PRIVATE KEY"), "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("加载 RSA 私钥失败", e);
        }
    }

    private static String pemBoundary(String position, String label) {
        return "-----" + position + " " + label + "-----";
    }

    private static RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) {
        try {
            if (privateKey instanceof RSAPrivateCrtKey crt) {
                RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent());
                return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(pubSpec);
            }
            throw new IllegalStateException("私钥不是 CRT 格式，无法派生公钥");
        } catch (Exception e) {
            throw new IllegalStateException("从私钥派生公钥失败", e);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("生成 RSA 密钥对失败", e);
        }
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return keyId;
    }
}
