package com.ai.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProtocolPasswordEncoderTest {

    private final ProtocolPasswordEncoder encoder = new ProtocolPasswordEncoder();

    @Test
    void encodesAndMatchesOnlyProtocolDigest() {
        String digest = PasswordDigestProtocol.sha1Utf8("test-password");
        String encoded = encoder.encode(digest);

        assertThat(encoded).startsWith("$2");
        assertThat(encoder.matches(digest, encoded)).isTrue();
        assertThat(encoder.matches(PasswordDigestProtocol.sha1Utf8("wrong-password"), encoded)).isFalse();
    }

    @Test
    void rejectsRawPasswordWrongEncodingAndDoubleHash() {
        String digest = PasswordDigestProtocol.sha1Utf8("test-password");
        String encoded = encoder.encode(digest);

        assertThatIllegalArgumentException().isThrownBy(() -> encoder.encode("test-password"));
        assertThatIllegalArgumentException().isThrownBy(() -> encoder.encode(digest.toUpperCase()));
        assertThat(encoder.matches("test-password", encoded)).isFalse();
        assertThat(encoder.matches(PasswordDigestProtocol.sha1Utf8(digest), encoded)).isFalse();
    }

    @Test
    void rejectsDigestOfAnEmptyPassword() {
        String emptyPasswordDigest = PasswordDigestProtocol.sha1Utf8("");

        assertThatIllegalArgumentException().isThrownBy(() -> encoder.encode(emptyPasswordDigest));
    }

    @Test
    void rejectsBcryptHashWithAnUnsupportedCost() {
        assertThat(PasswordDigestProtocol.isBcryptHash("$2b$04$" + "A".repeat(53))).isTrue();
        assertThat(PasswordDigestProtocol.isBcryptHash("$2b$31$" + "A".repeat(53))).isTrue();
        assertThat(PasswordDigestProtocol.isBcryptHash("$2b$03$" + "A".repeat(53))).isFalse();
        assertThat(PasswordDigestProtocol.isBcryptHash("$2b$32$" + "A".repeat(53))).isFalse();
    }

    @Test
    void acceptsExistingStandardBcryptAndUsesRandomSalt() {
        String digest = PasswordDigestProtocol.sha1Utf8("test-password");
        String first = encoder.encode(digest);
        String second = encoder.encode(digest);
        String existingBcrypt = new BCryptPasswordEncoder().encode(digest);

        assertThat(first).isNotEqualTo(second);
        assertThat(encoder.matches(digest, first)).isTrue();
        assertThat(encoder.matches(digest, existingBcrypt)).isTrue();
        assertThat(encoder.matches(digest, "{sha1-bcrypt}" + first)).isFalse();
    }
}
