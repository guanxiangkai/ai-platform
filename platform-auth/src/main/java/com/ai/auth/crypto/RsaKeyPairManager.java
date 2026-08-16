package com.ai.auth.crypto;

import com.ai.auth.properties.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * RSA 密钥对管理器（Security 模块专用）
 * <p>
 * 从受控运行配置加载 RSA 私钥；未配置时服务拒绝启动，
 * 为 JWT 签名服务提供签名私钥。
 * <br/>
 * ⚠️ 私钥仅在 Auth 服务内部使用，绝不对外暴露。
 * 公钥由部署流程同步配置到 Gateway 的受控配置中。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
@Slf4j
public class RsaKeyPairManager {

    private static final String PEM_DELIMITER = "-----";
    private static final String PKCS8_BEGIN = pemBoundary("BEGIN", "PRIVATE KEY");
    private static final String PKCS8_END = pemBoundary("END", "PRIVATE KEY");
    private static final String PKCS1_BEGIN = pemBoundary("BEGIN", "RSA PRIVATE KEY");
    private static final int MINIMUM_RSA_BITS = 2_048;

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;

    /**
     * 从 Auth 受控配置加载 PKCS#8 RSA 私钥并派生公钥。
     *
     * @param properties JWT 签发配置
     * @throws IllegalStateException 私钥缺失、格式错误、强度不足或无法派生公钥时抛出
     */
    public RsaKeyPairManager(JwtProperties properties) {
        this.keyId = properties.keyId();

        if (properties.privateKey() == null || properties.privateKey().isBlank()) {
            throw new IllegalStateException("未配置 ai.security.jwt.private-key，Auth 服务拒绝启动");
        }

        log.info("从受控配置加载 RSA 私钥, keyId={}", keyId);
        this.privateKey = loadPrivateKey(properties.privateKey());
        this.publicKey = derivePublicKey(this.privateKey);
        log.info("RSA 密钥对初始化完成, keyId={}", keyId);
    }

    // ==================== 私有方法 ====================

    private static RSAPrivateKey loadPrivateKey(String pem) {
        String normalized = pem.strip();
        if (normalized.contains(PKCS1_BEGIN)) {
            throw new IllegalStateException("ai.security.jwt.private-key 不支持 PKCS#1，必须提供 PKCS#8 RSA 私钥");
        }
        try {
            String cleaned = normalized
                    .replace(PKCS8_BEGIN, "")
                    .replace(PKCS8_END, "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            RSAPrivateKey loaded = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
            if (loaded.getModulus().bitLength() < MINIMUM_RSA_BITS) {
                throw new IllegalStateException("RSA 私钥强度不能低于 " + MINIMUM_RSA_BITS + " 位");
            }
            return loaded;
        } catch (Exception e) {
            if (e instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException("加载 RSA 私钥失败，仅支持 PKCS#8 PEM 或 Base64 内容", e);
        }
    }

    private static String pemBoundary(String action, String label) {
        return PEM_DELIMITER + action + " " + label + PEM_DELIMITER;
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

    /**
     * 返回仅供 Auth 签名使用的 RSA 私钥。
     *
     * @return RSA 私钥
     */
    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * 返回由签名私钥派生的 RSA 公钥。
     *
     * @return RSA 公钥
     */
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * 返回部署配置中的签名密钥标识。
     *
     * @return 签名密钥标识
     */
    public String getKeyId() {
        return keyId;
    }
}
