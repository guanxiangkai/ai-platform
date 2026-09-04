package com.ai.api.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 平台账户密码编码器。
 *
 * <p>输入为 {@link PasswordDigestProtocol} 规定的 SHA-1 摘要，存储格式为
 * {@code {sha1-bcrypt}<bcrypt>}。缺少标识的旧 BCrypt 值被明确拒绝，迁移必须由部署流程完成。</p>
 */
public final class ProtocolPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return PasswordDigestProtocol.BCRYPT_MARKER + bcrypt.encode(PasswordDigestProtocol.requireDigest(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        String digest;
        try {
            digest = PasswordDigestProtocol.requireDigest(rawPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (!PasswordDigestProtocol.isProtocolBcryptHash(encodedPassword)) {
            return false;
        }
        return bcrypt.matches(digest, encodedPassword.substring(PasswordDigestProtocol.BCRYPT_MARKER.length()));
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return !PasswordDigestProtocol.isProtocolBcryptHash(encodedPassword)
                || bcrypt.upgradeEncoding(encodedPassword.substring(PasswordDigestProtocol.BCRYPT_MARKER.length()));
    }
}
