package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Dict;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典数据传输对象。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "字典DTO")
@AutoMapper(target = Dict.class)
public record DictDTO(
        @Schema(description = "主键ID（更新时必填）") String id,
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "字典类型", requiredMode = Schema.RequiredMode.REQUIRED) String dictType,
        @Schema(description = "字典标签", requiredMode = Schema.RequiredMode.REQUIRED) String dictLabel,
        @Schema(description = "字典值", requiredMode = Schema.RequiredMode.REQUIRED) String dictValue
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
