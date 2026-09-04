package com.ai.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 平台密码传输协议。
 *
 * <p>客户端只能提交 UTF-8 原始密码的 SHA-1 小写十六进制摘要；服务端不得把原始密码、其他编码或
 * 已存储 BCrypt 值当作该协议输入。</p>
 */
public final class PasswordDigestProtocol {

    /** 密码摘要 wire 格式：40 位小写 SHA-1 十六进制字符串。 */
    public static final String DIGEST_REGEX = "^[0-9a-f]{40}$";
    /** UTF-8 空字符串的 SHA-1，格式正确但违反密码非空不变量。 */
    private static final String EMPTY_PASSWORD_DIGEST = sha1Utf8("");
    private static final Pattern DIGEST = Pattern.compile(DIGEST_REGEX);

    private PasswordDigestProtocol() {
    }

    /** 验证并返回规范化的密码摘要。 */
    public static String requireDigest(CharSequence value) {
        if (value == null || !DIGEST.matcher(value).matches() || EMPTY_PASSWORD_DIGEST.contentEquals(value)) {
            throw new IllegalArgumentException("密码摘要必须为40位小写SHA-1十六进制字符串");
        }
        return value.toString();
    }

    /** 判断值是否为标准裸 BCrypt 存储格式。 */
    public static boolean isBcryptHash(String encodedPassword) {
        return encodedPassword != null
                && encodedPassword.matches("^\\$2[aby]\\$(?:0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}$");
    }

    /** 仅供服务端生成的一次性随机密码转换为协议摘要；客户端不得提交原始密码。 */
    public static String sha1Utf8(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        try {
            byte[] result = MessageDigest.getInstance("SHA-1")
                    .digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境不支持SHA-1", exception);
        }
    }
}
