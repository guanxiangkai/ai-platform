package com.ai.sse.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.core.net.ClientIpResolver;
import io.github.guanxiangkai.web.plus.core.util.UserClaimsCodec;
import io.github.guanxiangkai.web.plus.error.exception.PermissionDeniedException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import com.ai.sse.service.ISseService;
import com.ai.sse.service.SseTicketService;
import com.ai.sse.service.SseTicketService.TicketInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


/**
 * SSE 推送控制器
 * <p>
 * 提供 SSE 长连接建立、断开以及在线状态查询接口。
 * 消息推送由 MQ 消费者驱动，不通过 HTTP 触发。
 * </p>
 * <p>
 * SSE 连接流程（Ticket 一次性令牌模式）：
 * <ol>
 *   <li>前端调用 {@code POST /sse/ticket}（携带 Authorization: Bearer xxx）获取一次性票据</li>
 *   <li>前端通过 {@code new EventSource("/sse/connect?ticket=uuid")} 建立连接</li>
 *   <li>SSE 服务验证并消费票据（30 秒过期、一次性使用、IP/UA 绑定校验），建立长连接</li>
 * </ol>
 * 避免在 URL 中暴露完整 JWT（日志泄露、Referer 泄露、浏览器历史记录风险）。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "SSE 推送", description = "SSE 长连接管理与在线状态查询")
@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {

    private final ISseService service;
    private final SseTicketService ticketService;
    private final ClientIpResolver clientIpResolver;

    // ─────────────────────── 连接管理 ───────────────────────

    /**
     * 获取 SSE 连接票据
     * <p>
     * 已认证端点（需要 Authorization: Bearer xxx），
     * 返回一次性短期票据用于建立 SSE 连接。
     * 票据绑定当前请求的 IP 和 User-Agent，防止被转用。
     * </p>
     */
    @Operation(summary = "获取 SSE 连接票据", description = "获取一次性短期票据（30 秒过期），用于建立 SSE 连接")
    @PostMapping("/ticket")
    public Mono<ApiResponse<String>> createTicket(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            AuthenticatedRequestUser currentUser = resolveAuthenticatedUser(exchange);
            String clientIp = clientIpResolver.resolve(exchange.getRequest());
            String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
            String ticket = ticketService.createTicket(currentUser.userId(), currentUser.tenantId(), clientIp, userAgent);
            return ApiResponse.ok(ticket);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 建立 SSE 长连接（票据认证 + 上下文校验）
     * <p>
     * 公开端点（无需 JWT），通过一次性票据验证身份，
     * 同时校验 IP 和 User-Agent 是否与创建票据时一致。
     * </p>
     */
    @Operation(summary = "建立 SSE 连接", description = "通过一次性票据建立 Server-Sent Events 长连接")
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> connect(
            @Parameter(description = "一次性连接票据", required = true) @RequestParam String ticket,
            ServerWebExchange exchange) {
        String clientIp = clientIpResolver.resolve(exchange.getRequest());
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        TicketInfo info = ticketService.validateAndConsume(ticket, clientIp, userAgent);
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "票据无效或已过期");
        }
        log.info("SSE 票据验证通过并准备建立连接");
        return service.connect(info.userId(), info.tenantId());
    }

    // ─────────────────────── 连接查询 ───────────────────────

    /**
     * 断开 SSE 连接
     * <p>只能断开自己的连接，超级管理员可断开任意连接。</p>
     */
    @Operation(summary = "断开 SSE 连接", description = "主动断开指定用户的 SSE 长连接，只能断开自己的连接")
    @PostMapping("/disconnect")
    public Mono<ApiResponse<Boolean>> disconnect(
            @Parameter(description = "用户ID", required = true) @RequestParam String userId,
            ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            AuthenticatedRequestUser currentUser = resolveAuthenticatedUser(exchange);
            if (!StringUtils.hasText(currentUser.userId())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未获取到当前登录用户");
            }
            if (!currentUser.userId().equals(userId) && !currentUser.superAdmin()) {
                throw new PermissionDeniedException("无权断开其他用户的连接");
            }
            service.disconnect(userId);
            return ApiResponse.ok(true);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 查询用户是否在线
     */
    @Operation(summary = "查询在线状态", description = "查询指定用户是否存在 SSE 连接")
    @GetMapping("/online/{userId}")
    public Mono<ApiResponse<Boolean>> isOnline(
            @Parameter(description = "用户ID") @PathVariable String userId) {
        return Mono.fromCallable(() -> ApiResponse.ok(service.isOnline(userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 在线连接总数
     */
    @Operation(summary = "在线总数", description = "查询当前所有在线 SSE 连接数")
    @GetMapping("/online/count")
    public Mono<ApiResponse<Integer>> getOnlineCount() {
        return Mono.fromCallable(() -> ApiResponse.ok(service.getOnlineCount()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 指定租户在线连接数
     */
    @Operation(summary = "租户在线数", description = "查询指定租户的在线 SSE 连接数")
    @GetMapping("/online/count/{tenantId}")
    public Mono<ApiResponse<Integer>> getOnlineCountByTenant(
            @Parameter(description = "租户ID") @PathVariable String tenantId) {
        return Mono.fromCallable(() -> ApiResponse.ok(service.getOnlineCountByTenant(tenantId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    // ─────────────────────── 私有方法 ───────────────────────

    private AuthenticatedRequestUser resolveAuthenticatedUser(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String userId = firstNonBlank(
                request.getHeaders().getFirst(AuthConstants.HeaderConstants.USER_ID),
                SecurityUtils.getUserId()
        );
        String tenantId = firstNonBlank(
                request.getHeaders().getFirst(AuthConstants.HeaderConstants.TENANT_ID),
                SecurityUtils.getTenantId()
        );
        boolean superAdmin = hasSuperAdminClaim(request) || SecurityUtils.isSuperAdmin();
        return new AuthenticatedRequestUser(userId, tenantId, superAdmin);
    }

    private boolean hasSuperAdminClaim(ServerHttpRequest request) {
        String rawClaims = request.getHeaders().getFirst(AuthConstants.HeaderConstants.USER_CLAIMS);
        if (!StringUtils.hasText(rawClaims)) {
            return false;
        }
        try {
            String claimsJson = UserClaimsCodec.decode(rawClaims,
                    request.getHeaders().getFirst(AuthConstants.HeaderConstants.USER_CLAIMS_ENCODING));
            JSONObject claims = JSONUtil.parseObj(claimsJson);
            Object superAdmin = claims.get("superAdmin");
            return superAdmin != null && Boolean.parseBoolean(superAdmin.toString());
        } catch (Exception e) {
            log.warn("解析 X-User-Claims 失败: exception={}", e.getClass().getSimpleName());
            return false;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private record AuthenticatedRequestUser(String userId, String tenantId, boolean superAdmin) {
    }
}
