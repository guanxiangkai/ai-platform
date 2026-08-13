package com.ai.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户角色分配请求DTO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "用户角色分配请求DTO")
public record UserRoleAssignDTO(
        @Schema(description = "角色ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "角色ID列表不能为空")
        List<String> roleIds,
        @Schema(description = "用户类型(ADMIN-管理员,USER-用户)")
        String userType
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
