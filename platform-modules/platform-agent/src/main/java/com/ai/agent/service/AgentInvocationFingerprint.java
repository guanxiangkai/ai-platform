package com.ai.agent.service;

import com.ai.agent.domain.dto.AgentInvocationRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * 为调用幂等校验生成稳定的请求指纹。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
final class AgentInvocationFingerprint {
    private AgentInvocationFingerprint() {
    }

    /** 对影响上游请求语义的字段生成 SHA-256 指纹。 */
    static String create(String agentId, AgentInvocationRequest request) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, agentId);
        append(canonical, request.sessionId());
        append(canonical, request.message());
        append(canonical, request.sessionTitle());
        append(canonical, request.contextNamespace());
        append(canonical, request.contextReference());
        appendValue(canonical, request.variables());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Java 运行时不支持 SHA-256", exception);
        }
    }

    private static void appendValue(StringBuilder target, Object value) {
        if (value == null) {
            target.append("null;");
            return;
        }
        if (value instanceof Map<?, ?> map) {
            target.append("map{");
            map.entrySet().stream()
                    .sorted((left, right) -> String.valueOf(left.getKey()).compareTo(String.valueOf(right.getKey())))
                    .forEach(entry -> {
                        append(target, String.valueOf(entry.getKey()));
                        appendValue(target, entry.getValue());
                    });
            target.append("};");
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            target.append("list[");
            iterable.forEach(item -> appendValue(target, item));
            target.append("];");
            return;
        }
        append(target, value.getClass().getName() + ':' + value);
    }

    private static void append(StringBuilder target, String value) {
        if (value == null) {
            target.append("-1:");
        } else {
            target.append(value.length()).append(':').append(value);
        }
        target.append(';');
    }
}
