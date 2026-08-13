package com.ai.api.system.log;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平台操作日志 Redis Stream 协议编解码器。
 *
 * <p>字段名、必填约束和类型转换仅在此处定义，发布端与持久化端共享同一协议。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class OperationLogStreamCodec {

    /** 操作日志 Stream 键。 */
    public static final String STREAM_KEY = "log:operation:stream";

    /** 平台持久化消费组。 */
    public static final String CONSUMER_GROUP = "platform-operation-log-group";

    /** 平台持久化消费者名称。 */
    public static final String CONSUMER_NAME = "platform-system-operation-log-consumer";

    /** 初始化记录字段。 */
    public static final String INITIALIZATION_FIELD = "init";

    /** 初始化记录值。 */
    public static final String INITIALIZATION_VALUE = "true";

    private OperationLogStreamCodec() {
    }

    /**
     * 将平台操作日志编码为 Stream 字段。
     *
     * @param log 当前操作日志
     * @return 可直接写入 Redis Stream 的有序字段
     */
    public static Map<String, String> encode(PlatformOperationLog log) {
        if (log == null) {
            throw new IllegalArgumentException("操作日志不能为空");
        }
        Map<String, String> values = new LinkedHashMap<>();
        put(values, "traceId", log.getTraceId());
        put(values, "userId", log.getUserId());
        put(values, "username", log.getUsername());
        put(values, "clientIp", log.getClientIp());
        put(values, "location", log.getLocation());
        put(values, "status", requiredLogField(log.getStatus(), "status"));
        put(values, "message", log.getMessage());
        put(values, "logTime", log.getLogTime() == null ? LocalDateTime.now() : log.getLogTime());
        put(values, "tenantId", requiredLogField(log.getTenantId(), "tenantId"));
        put(values, "operationId", requiredLogField(log.getOperationId(), "operationId"));
        put(values, "module", log.getModule());
        put(values, "operationTypeCode", log.getOperationTypeCode());
        put(values, "description", log.getDescription());
        put(values, "requestMethod", log.getRequestMethod());
        put(values, "requestUrl", log.getRequestUrl());
        put(values, "requestParams", log.getRequestParams());
        put(values, "responseData", log.getResponseData());
        put(values, "userAgent", log.getUserAgent());
        put(values, "costMs", log.getCostMs());
        put(values, "errorMessage", log.getErrorMessage());
        return values;
    }

    /**
     * 将 Stream 字段解码为类型安全记录。
     *
     * @param values Redis Stream 字段
     * @return 操作日志记录
     * @throws IllegalArgumentException 必填字段缺失或字段格式无效
     */
    public static OperationLogStreamRecord decode(Map<String, String> values) {
        if (values == null) {
            throw new IllegalArgumentException("操作日志 Stream 记录不能为空");
        }
        return new OperationLogStreamRecord(
                blankToNull(values.get("traceId")),
                blankToNull(values.get("userId")),
                blankToNull(values.get("username")),
                blankToNull(values.get("clientIp")),
                blankToNull(values.get("location")),
                required(values, "status"),
                blankToNull(values.get("message")),
                LocalDateTime.parse(required(values, "logTime")),
                required(values, "tenantId"),
                required(values, "operationId"),
                blankToNull(values.get("module")),
                blankToNull(values.get("operationTypeCode")),
                blankToNull(values.get("description")),
                blankToNull(values.get("requestMethod")),
                blankToNull(values.get("requestUrl")),
                blankToNull(values.get("requestParams")),
                blankToNull(values.get("responseData")),
                blankToNull(values.get("userAgent")),
                parseLong(values.get("costMs")),
                blankToNull(values.get("errorMessage"))
        );
    }

    /** 判断记录是否为创建消费组使用的初始化记录。 */
    public static boolean isInitializationRecord(Map<String, String> values) {
        return values != null && INITIALIZATION_VALUE.equals(values.get(INITIALIZATION_FIELD));
    }

    private static void put(Map<String, String> values, String key, Object value) {
        values.put(key, value == null ? "" : String.valueOf(value));
    }

    private static String required(Map<String, String> values, String key) {
        String value = blankToNull(values.get(key));
        if (value == null) {
            throw new IllegalArgumentException("操作日志 Stream 缺少字段：" + key);
        }
        return value;
    }

    private static String requiredLogField(String value, String key) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException("操作日志缺少必填字段：" + key);
        }
        return normalized;
    }

    private static Long parseLong(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : Long.valueOf(normalized);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 操作日志 Stream 的类型安全记录。
     *
     * @param traceId 链路追踪标识
     * @param userId 操作用户标识
     * @param username 操作用户名
     * @param clientIp 客户端 IP
     * @param location 客户端位置
     * @param status 执行状态
     * @param message 日志消息
     * @param logTime 日志发生时间
     * @param tenantId 所属租户标识
     * @param operationId 操作唯一标识
     * @param module 所属模块
     * @param operationTypeCode 操作类型编码
     * @param description 操作描述
     * @param requestMethod HTTP 请求方法
     * @param requestUrl 请求地址
     * @param requestParams 请求参数
     * @param responseData 响应数据
     * @param userAgent 客户端代理信息
     * @param costMs 执行耗时，单位为毫秒
     * @param errorMessage 错误信息
     */
    public record OperationLogStreamRecord(
            String traceId,
            String userId,
            String username,
            String clientIp,
            String location,
            String status,
            String message,
            LocalDateTime logTime,
            String tenantId,
            String operationId,
            String module,
            String operationTypeCode,
            String description,
            String requestMethod,
            String requestUrl,
            String requestParams,
            String responseData,
            String userAgent,
            Long costMs,
            String errorMessage
    ) {
    }
}
