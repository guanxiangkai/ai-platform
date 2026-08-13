package com.ai.api.system.log;

import io.github.guanxiangkai.web.plus.log.entity.BaseLog;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 跨服务操作日志事件。
 *
 * <p>Web Plus 负责填充字段，平台日志发布器负责写入 Redis Stream，
 * {@code platform-system} 负责持久化。</p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
public class PlatformOperationLog extends BaseLog {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 操作唯一标识。 */
    private String operationId;
    /** 所属模块。 */
    private String module;
    /** 操作类型编码。 */
    private String operationTypeCode;
    /** 操作描述。 */
    private String description;
    /** HTTP 请求方法。 */
    private String requestMethod;
    /** 请求地址。 */
    private String requestUrl;
    /** 请求参数。 */
    private String requestParams;
    /** 响应数据。 */
    private String responseData;
    /** 客户端代理信息。 */
    private String userAgent;
    /** 执行耗时，单位为毫秒。 */
    private Long costMs;
    /** 错误信息。 */
    private String errorMessage;
}
