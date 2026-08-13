package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 角色分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色分页查询参数")
public class RolePageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "角色编码（模糊查询）")
    private String roleCode;

    @Schema(description = "角色名称（模糊查询）")
    private String roleName;

    @Schema(description = "数据权限范围")
    private Integer dataScope;

    @Schema(description = "是否启用")
    private Boolean enabled;
}