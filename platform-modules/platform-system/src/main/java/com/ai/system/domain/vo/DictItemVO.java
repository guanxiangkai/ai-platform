package com.ai.system.domain.vo;

import com.ai.system.domain.entity.DictItem;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典项详情视图对象
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "字典项VO")
@AutoMapper(target = DictItem.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItemVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "字典ID")
    private String dictId;

    @Schema(description = "字典代码")
    private String dictCode;

    @Schema(description = "字典项值")
    private String itemValue;

    @Schema(description = "字典项标签")
    private String itemLabel;

    @Schema(description = "样式类型")
    private String itemStyle;

    @Schema(description = "字典颜色")
    private String itemColor;

    @Schema(description = "CSS样式类名")
    private String itemCssClass;

    @Schema(description = "是否默认选中")
    private Boolean itemSelected;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "是否启用(0:禁用,1:启用)")
    private Boolean enabled;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
