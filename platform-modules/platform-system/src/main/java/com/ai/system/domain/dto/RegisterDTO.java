package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Register;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 注册记录更新DTO
 * <p>
 * 管理员用于更新注册记录基本信息。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "注册记录更新DTO")
@AutoMapper(target = Register.class)
public record RegisterDTO(
        @Schema(description = "主键ID（更新时必填）") String id,
        @Schema(description = "备注") String remark,
        @Schema(description = "昵称") String nickname,
        @Schema(description = "真实姓名") String realName,
        @Schema(description = "邮箱") String email,
        @Schema(description = "手机号") String phone,
        @Schema(description = "性别(0-未知,1-男,2-女)") String gender
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
