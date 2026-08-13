package com.ai.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色权限分配请求DTO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "角色权限分配请求DTO")
public record RolePermissionAssignDTO(
        @Schema(description = "权限ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "权限ID列表不能为空")
        List<String> permissionIds
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
