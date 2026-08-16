package com.ai.auth.crypto;

import com.ai.auth.properties.JwtProperties;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RsaKeyPairManagerTest {

    @Test
    void missingPrivateKeyFailsAtStartup() {
        assertThatThrownBy(() -> new RsaKeyPairManager(properties(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ai.security.jwt.private-key");
    }

    @Test
    void loadsPkcs8PemAndDerivesMatchingPublicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        RSAPrivateKey privateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(privateKey.getEncoded());
        String pem = pemEnvelope("PRIVATE KEY", encoded);

        RsaKeyPairManager manager = new RsaKeyPairManager(properties(pem));

        assertThat(manager.getPrivateKey().getModulus()).isEqualTo(privateKey.getModulus());
        assertThat(manager.getPublicKey().getModulus()).isEqualTo(privateKey.getModulus());
        assertThat(manager.getKeyId()).isEqualTo("key-id");
    }

    @Test
    void rejectsUnsupportedPkcs1EnvelopeExplicitly() {
        String unsupported = pemEnvelope("RSA PRIVATE KEY", "AA==");

        assertThatThrownBy(() -> new RsaKeyPairManager(properties(unsupported)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PKCS#1")
                .hasMessageContaining("PKCS#8");
    }

    @Test
    void rejectsWeakRsaKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        String encoded = Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded());

        assertThatThrownBy(() -> new RsaKeyPairManager(properties(encoded)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2048");
    }

    private static JwtProperties properties(String privateKey) {
        return new JwtProperties(
                privateKey,
                "key-id",
                Duration.ofHours(2),
                Duration.ofDays(7),
                "issuer",
                "gateway-audience",
                "auth-audience",
                Duration.ofSeconds(60)
        );
    }

    private static String pemEnvelope(String label, String body) {
        String delimiter = "-----";
        return delimiter + "BEGIN " + label + delimiter + "\n"
                + body + "\n"
                + delimiter + "END " + label + delimiter;
    }
}
