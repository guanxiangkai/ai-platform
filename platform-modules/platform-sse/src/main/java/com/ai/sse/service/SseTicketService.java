package com.ai.sse.service;

import com.ai.sse.config.SseProperties;
import com.ai.sse.constants.SseConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * SSE 连接票据服务
 * <p>
 * 解决 EventSource 无法设置自定义请求头的问题：
 * 前端先通过已认证的 API 获取一次性短期票据，再用票据建立 SSE 连接。
 * </p>
 * <p>
 * 安全特性：
 * <ul>
 *   <li>票据为不可猜测的 UUID，不含任何用户敏感信息</li>
 *   <li>按受控配置自动过期（Redis TTL）</li>
 *   <li>一次性消费（{@code getAndDelete}），不可重放</li>
 *   <li>绑定客户端上下文（IP + UA Hash），防止票据被转用</li>
 * </ul>
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseTicketService {

    private final StringRedisTemplate redisTemplate;
    private final SseProperties sseProperties;

    /**
     * 创建一次性票据（绑定客户端上下文）
     *
     * @param userId    当前认证用户 ID
     * @param tenantId  当前租户 ID（可选）
     * @param clientIp  客户端 IP
     * @param userAgent 客户端 User-Agent
     * @return 票据字符串（UUID）
     */
    public String createTicket(String userId, String tenantId, String clientIp, String userAgent) {
        Assert.hasText(userId, "userId 不能为空");
        String ticket = UUID.randomUUID().toString();

        // 构建票据值：userId|tenantId|ipHash|uaHash
        StringBuilder value = new StringBuilder();
        value.append(userId).append(SseConstants.TicketConstants.VALUE_SEPARATOR);
        value.append(tenantId != null ? tenantId : "").append(SseConstants.TicketConstants.VALUE_SEPARATOR);

        if (sseProperties.ticketBindContext()) {
            value.append(hashValue(clientIp)).append(SseConstants.TicketConstants.VALUE_SEPARATOR);
            value.append(hashValue(userAgent));
        }

        redisTemplate.opsForValue().set(
                SseConstants.RedisKeyConstants.TICKET_PREFIX + ticket,
                value.toString(),
                sseProperties.ticketTtl());
        log.debug("SSE 票据已创建: bindContext={}, ttl={}",
                sseProperties.ticketBindContext(), sseProperties.ticketTtl());
        return ticket;
    }

    /**
     * 验证并消费票据（一次性 + 上下文校验）
     *
     * @param ticket    票据字符串
     * @param clientIp  请求方 IP
     * @param userAgent 请求方 User-Agent
     * @return 票据信息（userId + tenantId），无效返回 null
     */
    public TicketInfo validateAndConsume(String ticket, String clientIp, String userAgent) {
        if (!StringUtils.hasText(ticket)) {
            return null;
        }
        String value = redisTemplate.opsForValue().getAndDelete(SseConstants.RedisKeyConstants.TICKET_PREFIX + ticket);
        if (value == null) {
            log.debug("SSE 票据无效或已过期");
            return null;
        }
        String[] parts = value.split("\\" + SseConstants.TicketConstants.VALUE_SEPARATOR, -1);
        String userId = parts[0];
        String tenantId = parts.length > 1 && StringUtils.hasText(parts[1]) ? parts[1] : null;

        // 上下文校验（IP + UA Hash）
        if (sseProperties.ticketBindContext() && parts.length >= 4) {
            String storedIpHash = parts[2];
            String storedUaHash = parts[3];
            String currentIpHash = hashValue(clientIp);
            String currentUaHash = hashValue(userAgent);

            if (StringUtils.hasText(storedIpHash) && !storedIpHash.equals(currentIpHash)) {
                log.warn("SSE 票据客户端地址不匹配");
                return null;
            }
            if (StringUtils.hasText(storedUaHash) && !storedUaHash.equals(currentUaHash)) {
                log.warn("SSE 票据客户端标识不匹配");
                return null;
            }
        }

        log.debug("SSE 票据验证通过");
        return new TicketInfo(userId, tenantId);
    }

    /**
     * SHA-256 哈希（取前 16 位，节省 Redis 存储）
     */
    private String hashValue(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // Java 运行时保证提供 SHA-256；异常表示运行环境不完整。
            throw new IllegalStateException("运行环境不支持 SHA-256", e);
        }
    }

    /**
     * 票据携带的用户身份信息
     */
    public record TicketInfo(String userId, String tenantId) {
    }
}
