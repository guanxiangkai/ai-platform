package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Role;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色数据传输对象。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "角色DTO")
@AutoMapper(target = Role.class)
public record RoleDTO(
        @Schema(description = "主键ID（更新时必填）") String id,
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED) String roleCode,
        @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED) String roleName,
        @Schema(description = "数据权限范围") Integer dataScope,
        @Schema(description = "是否为默认注册角色(注册审核兜底)") Boolean defaultRegistrationRole,
        @Schema(description = "权限ID列表") List<String> permissionIds
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
