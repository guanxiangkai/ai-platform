package com.ai.api.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProtocolPasswordEncoderTest {

    private final ProtocolPasswordEncoder encoder = new ProtocolPasswordEncoder();

    @Test
    void encodesAndMatchesOnlyProtocolDigest() {
        String digest = PasswordDigestProtocol.sha1Utf8("test-password");
        String encoded = encoder.encode(digest);

        assertThat(encoded).startsWith(PasswordDigestProtocol.BCRYPT_MARKER);
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
    void rejectsUnmarkedLegacyBcryptAndUsesRandomSalt() {
        String digest = PasswordDigestProtocol.sha1Utf8("test-password");
        String first = encoder.encode(digest);
        String second = encoder.encode(digest);

        assertThat(first).isNotEqualTo(second);
        assertThat(encoder.matches(digest, first.substring(PasswordDigestProtocol.BCRYPT_MARKER.length()))).isFalse();
    }
}
