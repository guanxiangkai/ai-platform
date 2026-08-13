package com.ai.system.domain.dto;

import com.ai.system.domain.MenuType;
import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 菜单分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "菜单分页查询参数")
public class MenuPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "父菜单ID")
    private String parentId;

    @Schema(description = "菜单名称（模糊查询）")
    private String menuName;

    @Schema(description = "菜单标题（模糊查询）")
    private String menuTitle;

    @Schema(description = "菜单类型")
    private MenuType menuType;

    @Schema(description = "是否可见(0:隐藏,1:显示)")
    private Boolean visible;
}
