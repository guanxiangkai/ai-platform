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

        // 检查 URL 原始 query
        String rawQuery = request.getURI().getRawQuery();
        if (rawQuery != null) {
            String threat = detectThreat(rawQuery);
            if (threat != null) {
                log.warn("[安全] 请求 [{}] 查询参数中检测到 {}: {}", path, threat, rawQuery);
                return ReactiveResponseUtils.writeError(exchange, HttpStatus.FORBIDDEN, "请求包含非法字符");
            }
        }

        // 逐个检查 query 参数值
        for (Map.Entry<String, List<String>> entry : request.getQueryParams().entrySet()) {
            for (String value : entry.getValue()) {
                String threat = detectThreat(value);
                if (threat != null) {
                    log.warn("[安全] 请求 [{}] 参数 [{}] 检测到 {}: {}", path, entry.getKey(), threat, value);
                    return ReactiveResponseUtils.writeError(exchange, HttpStatus.FORBIDDEN, "请求参数包含非法字符");
                }
            }
        }

        return chain.filter(exchange);
    }

    // ==================== 私有方法 ====================

    private String detectThreat(String input) {
        if (input == null || input.isBlank()) return null;
        for (Pattern p : XSS_PATTERNS) {
            if (p.matcher(input).find()) return "XSS";
        }
        for (Pattern p : SQL_PATTERNS) {
            if (p.matcher(input).find()) return "SQL_INJECTION";
        }
        return null;
    }

    private boolean isExcluded(String path) {
        return GatewayPathMatcher.matchesAny(props.getXssExcludePaths(), path);
    }
}
