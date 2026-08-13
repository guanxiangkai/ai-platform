package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 字典项分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典项分页查询参数")
public class DictItemPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字典ID")
    private String dictId;

    @Schema(description = "字典代码")
    private String dictCode;

    @Schema(description = "字典项标签（模糊查询）")
    private String itemLabel;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "是否默认选中")
    private Boolean itemSelected;
}
