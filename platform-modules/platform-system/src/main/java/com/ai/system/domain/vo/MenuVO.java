package com.ai.system.domain.vo;

import io.github.guanxiangkai.web.plus.core.tree.TreeNode;

import io.github.guanxiangkai.web.plus.core.domain.vo.DataVO;
import com.ai.system.domain.MenuType;
import com.ai.system.domain.entity.Menu;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 菜单视图对象
 * <p>
 * 继承 {@link DataVO}，复用 id / createTime / updateTime / remark / enabled / sortOrder
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "菜单VO")
@AutoMapper(target = Menu.class)
public class MenuVO extends DataVO implements TreeNode<String, MenuVO> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "父菜单ID")
    private String parentId;

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

    @Schema(description = "前端组件注册键")
    private String component;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "是否可见")
    private Boolean visible;

    @Schema(description = "是否缓存")
    private Boolean keepAlive;

    @Schema(description = "是否外链")
    private Boolean isExternal;

    @Schema(description = "子菜单")
    private List<MenuVO> children;
}
