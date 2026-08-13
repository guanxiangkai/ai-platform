package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 操作日志分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志分页查询参数")
public class OperationLogPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "操作类型（QUERY/INSERT/UPDATE/DELETE/IMPORT/EXPORT 等）")
    private String operationTypeCode;

    @Schema(description = "用户名（模糊查询）")
    private String username;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "执行状态（SUCCESS/FAIL）")
    private String status;

    @Schema(description = "所属模块（模糊查询）")
    private String module;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
