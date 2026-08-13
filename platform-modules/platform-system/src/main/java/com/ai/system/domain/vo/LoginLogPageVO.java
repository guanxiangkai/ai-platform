package com.ai.system.domain.vo;

import io.github.guanxiangkai.web.plus.core.annotation.DictField;
import com.ai.system.constants.SystemConstants;
import com.ai.system.domain.entity.LoginLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志分页列表 VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录日志分页VO")
@AutoMapper(target = LoginLog.class)
public class LoginLogPageVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "登录动作（LOGIN/LOGOUT）")
    private String action;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "地理位置")
    private String location;

    @Schema(description = "登录状态")
    @DictField(type = SystemConstants.DictTypeConstants.SYS_COMMON_STATUS)
    private String status;

    @Schema(description = "登录状态名称")
    private String statusLabel;

    @Schema(description = "消息/错误提示")
    private String message;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "登录时间")
    private LocalDateTime logTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
