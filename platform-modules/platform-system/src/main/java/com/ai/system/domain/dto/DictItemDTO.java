package com.ai.system.domain.dto;

import com.ai.system.domain.entity.DictItem;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典项DTO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "字典项DTO")
@AutoMapper(target = DictItem.class)
public record DictItemDTO(
        @Schema(description = "字典ID") String dictId,
        @Schema(description = "字典代码") String dictCode,
        @Schema(description = "字典项值") String itemValue,
        @Schema(description = "字典项标签") String itemLabel,
        @Schema(description = "样式类型") String itemStyle,
        @Schema(description = "字典颜色") String itemColor,
        @Schema(description = "CSS样式类名") String itemCssClass,
        @Schema(description = "是否默认选中") Boolean itemSelected,
        @Schema(description = "排序") Integer sortOrder,
        @Schema(description = "是否启用(0:禁用,1:启用)") Boolean enabled,
        @Schema(description = "备注") String remark
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
