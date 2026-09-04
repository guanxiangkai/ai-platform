package com.ai.api.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 平台账户密码编码器。
 *
 * <p>输入为 {@link PasswordDigestProtocol} 规定的 SHA-1 摘要，存储格式为
 * 标准裸 BCrypt。存量哈希与新写入使用同一格式，不通过存储格式推断摘要算法。</p>
 */
public final class ProtocolPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(PasswordDigestProtocol.requireDigest(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        String digest;
        try {
            digest = PasswordDigestProtocol.requireDigest(rawPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (!PasswordDigestProtocol.isBcryptHash(encodedPassword)) {
            return false;
        }
        return bcrypt.matches(digest, encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return !PasswordDigestProtocol.isBcryptHash(encodedPassword) || bcrypt.upgradeEncoding(encodedPassword);
    }
}
