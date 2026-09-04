package com.ai.system.domain.dto;

import com.ai.api.security.PasswordDigestProtocol;
import com.ai.system.domain.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户更新数据传输对象。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "用户DTO")
@AutoMapper(target = User.class)
public record UserDTO(
        @Schema(description = "主键ID（更新时必填）") String id,
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED) String username,
        @Schema(description = "可选的新密码SHA-1小写十六进制摘要", accessMode = Schema.AccessMode.WRITE_ONLY) @Pattern(regexp = PasswordDigestProtocol.DIGEST_REGEX, message = "密码摘要必须为40位小写SHA-1十六进制字符串") @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String passwordDigest,
        @Schema(description = "昵称") String nickname,
        @Schema(description = "真实姓名") String realName,
        @Schema(description = "邮箱") String email,
        @Schema(description = "手机号") String phone,
        @Schema(description = "性别", ref = "Gender") Integer gender,
        @Schema(description = "头像") String avatar,
        @Schema(description = "用户类型(ADMIN-管理员,USER-用户)") String userType,
        @Schema(description = "角色编码列表") List<String> roleCodes,
        @Schema(description = "岗位编码列表") List<String> postCodes
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "UserDTO[id=" + id + ", username=" + username + ", passwordDigest=[REDACTED]]";
    }
}
