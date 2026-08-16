package com.ai.system.domain.dto;

import com.ai.system.domain.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户创建数据传输对象，密码为必填项。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "用户创建DTO")
@AutoMapper(target = User.class)
public record UserCreateDTO(
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "用户名不能为空") @Size(min = 2, max = 64, message = "用户名长度为2-64个字符") String username,
        @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度为6-64个字符") String password,
        @Schema(description = "昵称") @Size(max = 64, message = "昵称长度不能超过64个字符") String nickname,
        @Schema(description = "真实姓名") @Size(max = 64, message = "真实姓名长度不能超过64个字符") String realName,
        @Schema(description = "邮箱") @Email(message = "邮箱格式不正确") @Size(max = 128, message = "邮箱长度不能超过128个字符") String email,
        @Schema(description = "手机号") @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        @Schema(description = "性别", ref = "Gender") Integer gender,
        @Schema(description = "头像") String avatar,
        @Schema(description = "用户类型(ADMIN-管理员,USER-用户)") String userType,
        @Schema(description = "角色编码列表") List<String> roleCodes,
        @Schema(description = "岗位编码列表") List<String> postCodes
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 返回不包含用户名、密码或个人信息的诊断摘要。 */
    @Override
    public String toString() {
        return "UserCreateDTO[credentials=<redacted>]";
    }
}
