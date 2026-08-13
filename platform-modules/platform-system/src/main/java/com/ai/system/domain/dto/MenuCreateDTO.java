package com.ai.system.domain.dto;

import com.ai.system.domain.MenuType;
import com.ai.system.domain.entity.Menu;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜单DTO创建DTO
 * <p>
 * 用于创建接口入参，包含参数校验；更新接口使用 {@link MenuDTO}
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "菜单DTO创建DTO")
@AutoMapper(target = Menu.class)
public record MenuCreateDTO(
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "父菜单ID") String parentId,
        @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "菜单名称不能为空") @Size(max = 128, message = "菜单名称长度不能超过128个字符") String menuName,
        @Schema(description = "菜单标题") @Size(max = 128, message = "菜单标题长度不能超过128个字符") String menuTitle,
        @Schema(description = "菜单类型", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "菜单类型不能为空") MenuType menuType,
        @Schema(description = "路由路径") @Size(max = 255, message = "路由路径长度不能超过255个字符") String path,
        @Schema(description = "前端组件注册键") @Size(max = 255, message = "组件注册键长度不能超过255个字符") String component,
        @Schema(description = "权限标识") @Size(max = 128, message = "权限标识长度不能超过128个字符") String permission,
        @Schema(description = "图标") @Size(max = 128, message = "图标长度不能超过128个字符") String icon,
        Boolean visible,
        Boolean keepAlive,
        Boolean isExternal
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
