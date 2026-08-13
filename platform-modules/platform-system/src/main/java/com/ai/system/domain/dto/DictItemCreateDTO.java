package com.ai.system.domain.dto;

import com.ai.system.domain.entity.DictItem;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典项DTO创建DTO
 * <p>
 * 用于创建接口入参，包含参数校验；更新接口使用 {@link DictItemDTO}
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "字典项DTO创建DTO")
@AutoMapper(target = DictItem.class)
public record DictItemCreateDTO(
        @Schema(description = "字典ID") @NotBlank(message = "字典ID不能为空") String dictId,
        @Schema(description = "字典代码") String dictCode,
        @Schema(description = "字典项值") @NotBlank(message = "字典项值不能为空") String itemValue,
        @Schema(description = "字典项标签") @NotBlank(message = "字典项标签不能为空") String itemLabel,
        @Schema(description = "样式类型") String itemStyle,
        @Schema(description = "字典颜色") String itemColor,
        @Schema(description = "CSS样式类名") String itemCssClass,
        @Schema(description = "是否默认选中") Boolean itemSelected,
        @Schema(description = "排序") Integer sortOrder,
        Boolean enabled,
        @Schema(description = "备注") String remark
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
