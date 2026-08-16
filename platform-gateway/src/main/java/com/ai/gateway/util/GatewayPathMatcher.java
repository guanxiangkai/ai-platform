package com.ai.gateway.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.PathContainer;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 网关路径匹配工具。
 * <p>
 * 统一使用 WebFlux / Gateway 同源的 {@link PathPatternParser}，避免安全白名单、
 * JWT 跳过和 XSS 排除使用不同路径匹配语义。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GatewayPathMatcher {

    private static final PathPatternParser PARSER = new PathPatternParser();
    private static final ConcurrentMap<String, Optional<PathPattern>> CACHE = new ConcurrentHashMap<>();

    public static boolean matchesAny(Collection<String> patterns, String path) {
        if (patterns == null || patterns.isEmpty() || !StringUtils.hasText(path)) {
            return false;
        }
        PathContainer pathContainer = PathContainer.parsePath(path);
        return patterns.stream()
                .filter(StringUtils::hasText)
                .anyMatch(pattern -> matches(pattern, pathContainer));
    }

    private static boolean matches(String pattern, PathContainer path) {
        return CACHE.computeIfAbsent(pattern, GatewayPathMatcher::parsePattern)
                .map(pathPattern -> pathPattern.matches(path))
                .orElse(false);
    }

    private static Optional<PathPattern> parsePattern(String pattern) {
        try {
            return Optional.of(PARSER.parse(pattern));
        } catch (PatternParseException ex) {
            log.warn("[GatewayPathMatcher] 忽略非法路径模式: pattern={}, exception={}",
                    pattern, ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
