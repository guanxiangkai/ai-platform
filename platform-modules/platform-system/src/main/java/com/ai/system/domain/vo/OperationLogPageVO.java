package com.ai.system.domain.vo;

import io.github.guanxiangkai.web.plus.core.annotation.DictField;
import com.ai.system.constants.SystemConstants;
import com.ai.system.domain.entity.OperationLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志分页列表 VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "操作日志分页VO")
@AutoMapper(target = OperationLog.class)
public class OperationLogPageVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "操作类型")
    @DictField(type = SystemConstants.DictTypeConstants.SYS_OPERATION_TYPE)
    private String operationTypeCode;

    @Schema(description = "操作类型名称")
    private String operationTypeCodeLabel;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "地理位置")
    private String location;

    @Schema(description = "执行状态")
    @DictField(type = SystemConstants.DictTypeConstants.SYS_COMMON_STATUS)
    private String status;

    @Schema(description = "执行状态名称")
    private String statusLabel;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "操作描述")
    private String description;

    @Schema(description = "执行时长（ms）")
    private Long costMs;

    @Schema(description = "操作时间")
    private LocalDateTime logTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
