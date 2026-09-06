package com.ai.gateway.filter;

import com.ai.gateway.config.AiGatewayProperties;
import com.ai.gateway.constant.FilterOrder;
import com.ai.gateway.util.GatewayPathMatcher;
import com.ai.gateway.util.ReactiveResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 网关全局安全过滤器（XSS / SQL 注入防护）
 * <p>
 * 检查 URL 查询参数中是否包含恶意脚本或 SQL 注入片段。
 * Body 内容的 XSS 防护由各下游服务自行处理（Validation 框架）。
 * </p>
 * 错误响应写入复用 {@link ReactiveResponseUtils}。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityFilter implements GlobalFilter, Ordered {

    private static final int MAX_LOG_PATH_LENGTH = 256;
    private static final int MAX_LOG_PARAMETER_NAME_LENGTH = 96;
    private static final int MAX_QUERY_COMPONENT_LENGTH = 8_192;
    private static final String RAW_QUERY_STRUCTURE = "原始查询结构";

    /**
     * XSS 危险模式
     */
    private static final List<Pattern> XSS_PATTERNS = List.of(
            Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(^|[\\s<\"'`/])on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\balert\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\beval\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE)
    );
    /**
     * SQL 注入危险模式
     */
    private static final List<Pattern> SQL_PATTERNS = List.of(
            Pattern.compile("('\\s*(or|and)\\s+.*=)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(union\\s+select)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(insert\\s+into)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(drop\\s+table)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(delete\\s+from)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(;\\s*shutdown)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(--\\s)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(/\\*.*\\*/)", Pattern.CASE_INSENSITIVE)
    );
    private final AiGatewayProperties props;

    @Override
    public int getOrder() {
        return FilterOrder.SECURITY;
    }

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!props.isXssEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        // ServerHttpRequest 已将查询参数解析并解码；检测使用规范化后的参数名和值，日志绝不记录值。
        DetectedQueryThreat parameterThreat = detectQueryParameterThreat(request.getQueryParams());

        // 保留对未被参数解析覆盖的原始查询结构检测，且不将原始内容写入日志。
        String rawQuery = request.getURI().getRawQuery();
        String threat = detectRawQueryThreat(rawQuery);
        if (threat != null) {
            logDetectedThreat(path, parameterThreat == null ? RAW_QUERY_STRUCTURE : parameterThreat.parameterName(), threat);
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.FORBIDDEN, "请求包含非法字符");
        }

        if (parameterThreat != null) {
            logDetectedThreat(path, parameterThreat.parameterName(), parameterThreat.threat());
            return ReactiveResponseUtils.writeError(exchange, HttpStatus.FORBIDDEN, "请求参数包含非法字符");
        }

        return chain.filter(exchange);
    }

    // ==================== 私有方法 ====================

    private String detectThreat(String input) {
        if (input == null) return null;
        if (input.length() > MAX_QUERY_COMPONENT_LENGTH) return "QUERY_PARAMETER_TOO_LONG";
        if (input.isBlank()) return null;
        for (Pattern p : XSS_PATTERNS) {
            if (p.matcher(input).find()) return "XSS";
        }
        for (Pattern p : SQL_PATTERNS) {
            if (p.matcher(input).find()) return "SQL_INJECTION";
        }
        return null;
    }

    private String detectRawQueryThreat(String rawQuery) {
        if (rawQuery == null || rawQuery.length() > MAX_QUERY_COMPONENT_LENGTH) {
            return null;
        }
        return detectThreat(rawQuery);
    }

    private DetectedQueryThreat detectQueryParameterThreat(Map<String, List<String>> queryParameters) {
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            String parameterName = entry.getKey();
            String threat = detectThreat(parameterName);
            if (threat != null) {
                return new DetectedQueryThreat(parameterName, threat);
            }

            for (String value : entry.getValue()) {
                threat = detectThreat(value);
                if (threat != null) {
                    return new DetectedQueryThreat(parameterName, threat);
                }
            }
        }
        return null;
    }

    /**
     * 记录不含请求值的安全审计信息，避免敏感查询内容进入日志。
     *
     * @param path 请求路径
     * @param parameterName 检测到威胁的参数名或查询结构标识
     * @param threat 威胁类型
     */
    private void logDetectedThreat(String path, String parameterName, String threat) {
        log.warn("[安全] 请求 [{}] 参数 [{}] 检测到 {}", sanitizeLogField(path, MAX_LOG_PATH_LENGTH),
                sanitizeLogField(parameterName, MAX_LOG_PARAMETER_NAME_LENGTH), threat);
    }

    private String sanitizeLogField(String input, int maximumLength) {
        if (input == null || input.isEmpty()) {
            return "（空）";
        }

        StringBuilder sanitized = new StringBuilder(Math.min(input.length(), maximumLength));
        int offset = 0;
        int length = 0;
        while (offset < input.length() && length < maximumLength) {
            int codePoint = input.codePointAt(offset);
            int characterType = Character.getType(codePoint);
            boolean unsafeForSingleLineLog = Character.isISOControl(codePoint)
                    || characterType == Character.LINE_SEPARATOR
                    || characterType == Character.PARAGRAPH_SEPARATOR
                    || characterType == Character.FORMAT;
            sanitized.appendCodePoint(unsafeForSingleLineLog ? '?' : codePoint);
            offset += Character.charCount(codePoint);
            length++;
        }
        if (offset < input.length()) {
            sanitized.append('…');
        }
        return sanitized.toString();
    }

    private boolean isExcluded(String path) {
        return GatewayPathMatcher.matchesAny(props.getXssExcludePaths(), path);
    }

    private record DetectedQueryThreat(String parameterName, String threat) {
    }
}
