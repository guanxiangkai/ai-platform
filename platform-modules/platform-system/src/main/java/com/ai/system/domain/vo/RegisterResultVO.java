package com.ai.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 注册申请提交结果
 * <p>
 * 注册成功后返回系统自动生成的用户名，请妥善保存，登录时需要使用该账号名。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "注册申请提交结果")
public record RegisterResultVO(

        @Schema(description = "注册记录ID")
        String id,

        @Schema(description = "系统为您生成的账号名，请牢记，登录时需要使用此账号")
        String username,

        @Schema(description = "匹配到的目录主体名称（确认信息）")
        String directoryDisplayName

) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
