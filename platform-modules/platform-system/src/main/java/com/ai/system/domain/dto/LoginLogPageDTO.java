package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 登录日志分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "登录日志分页查询参数")
public class LoginLogPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "登录动作（LOGIN/LOGOUT）")
    private String action;

    @Schema(description = "用户名（模糊查询）")
    private String username;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "登录状态（SUCCESS/FAIL）")
    private String status;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
