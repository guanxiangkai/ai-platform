package com.ai.system.domain.vo;

import com.ai.system.domain.entity.DictItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典项分页列表VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "字典项分页VO")
@AutoMapper(target = DictItem.class)
public class DictItemPageVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "排序号")
    private Integer sortOrder;

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

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
