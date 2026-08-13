package com.ai.system.domain.dto;

import com.ai.system.domain.entity.LoginLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录日志 DTO（日志由切面自动写入，此 DTO 仅用于 BaseController/IBaseService 泛型约束）
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "登录日志DTO")
@AutoMapper(target = LoginLog.class)
public record LoginLogDTO(
        @Schema(description = "登录状态") String status,
        @Schema(description = "消息") String message
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
