package com.ai.system.domain.vo;

import io.github.guanxiangkai.web.plus.core.domain.vo.BasePageVO;
import com.ai.system.domain.MenuType;
import com.ai.system.domain.entity.Menu;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 菜单分页列表视图对象
 * <p>
 * 继承 {@link BasePageVO}，复用 id
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "菜单分页VO")
@AutoMapper(target = Menu.class)
public class MenuPageVO extends BasePageVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单标题")
    private String menuTitle;

    @Schema(description = "菜单类型")
    private MenuType menuType;

    @Schema(description = "菜单类型名称")
    private String typeLabel;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "是否可见")
    private Boolean visible;

    @Schema(description = "排序")
    private Integer sortOrder;
}
