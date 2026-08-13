package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 字典分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典分页查询参数")
public class DictPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字典类型（模糊查询）")
    private String dictType;

    @Schema(description = "字典标签（模糊查询）")
    private String dictLabel;

    @Schema(description = "字典值（模糊查询）")
    private String dictValue;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
