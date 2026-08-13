package com.ai.auth.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 登录响应
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "登录响应")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        @Schema(description = "访问令牌（JWT Token）") String accessToken,
        @Schema(description = "刷新令牌") String refreshToken,
        @Schema(description = "令牌类型", example = "Bearer") String tokenType,
        @Schema(description = "过期时间（秒）", example = "7200") Long expiresIn,
        @Schema(description = "用户ID") String userId,
        @Schema(description = "用户名") String username,
        @Schema(description = "姓名") String name,
        @Schema(description = "头像") String avatar,
        @Schema(description = "用户类型") String userType,
        @Schema(description = "是否管理员") Boolean superAdmin,
        @Schema(description = "角色数组（角色编码）") Set<String> roleCodes,
        @Schema(description = "岗位数组（岗位编码）") Set<String> postCodes,
        @Schema(description = "权限列表（权限码）") Set<String> permissions,
        @Schema(description = "部门") String deptId,
        @Schema(description = "部门列表") Set<String> deptIds
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
