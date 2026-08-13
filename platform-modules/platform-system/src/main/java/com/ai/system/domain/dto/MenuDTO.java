package com.ai.system.domain.dto;

import com.ai.system.domain.MenuType;
import com.ai.system.domain.entity.Menu;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜单更新数据传输对象。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "菜单DTO")
@AutoMapper(target = Menu.class)
public record MenuDTO(
        @Schema(description = "主键ID（更新时必填）") String id,
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "父菜单ID") String parentId,
        @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED) String menuName,
        @Schema(description = "菜单标题") String menuTitle,
        @Schema(description = "菜单类型", requiredMode = Schema.RequiredMode.REQUIRED) MenuType menuType,
        @Schema(description = "路由路径") String path,
        @Schema(description = "前端组件注册键") String component,
        @Schema(description = "权限标识") String permission,
        @Schema(description = "图标") String icon,
        @Schema(description = "是否可见(0:隐藏,1:显示)") Boolean visible,
        @Schema(description = "是否缓存(0:不缓存,1:缓存)") Boolean keepAlive,
        @Schema(description = "是否外链") Boolean isExternal
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
