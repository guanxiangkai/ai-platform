package com.ai.files.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 文件对象键与内容完整性使用的 SHA-256 摘要边界。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
final class FileChecksum {
    private FileChecksum() {
    }

    /** 计算 UTF-8 文本的 SHA-256 十六进制摘要。 */
    static String sha256(String value) {
        return HexFormat.of().formatHex(digest().digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    /** 流式计算文件的 SHA-256 十六进制摘要。 */
    static String sha256(Path path) throws IOException {
        MessageDigest digest = digest();
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Java 运行时不支持 SHA-256", exception);
        }
    }
}
