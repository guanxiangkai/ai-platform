package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Register;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户注册申请DTO
 * <p>
 * 用于用户自助注册接口，无需认证即可调用。<br>
 * 无需填写用户名，系统将根据真实姓名自动生成并在注册成功后返回给用户。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "用户注册申请DTO")
@AutoMapper(target = Register.class)
public record RegisterCreateDTO(

        @Schema(description = "真实姓名", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "真实姓名不能为空")
        @Size(max = 64, message = "真实姓名长度不能超过64个字符")
        String realName,

        @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度为6-64个字符")
        String password,

        @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "请选择所在部门")
        String deptId,

        @Schema(description = "昵称")
        @Size(max = 64, message = "昵称长度不能超过64个字符")
        String nickname,

        @Schema(description = "邮箱")
        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱长度不能超过128个字符")
        String email,

        @Schema(description = "手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Schema(description = "性别(0-未知,1-男,2-女)")
        String gender

) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
