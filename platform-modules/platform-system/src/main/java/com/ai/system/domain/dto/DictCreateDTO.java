package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Dict;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典DTO创建DTO
 * <p>
 * 用于创建接口入参，包含参数校验；更新接口使用 {@link DictDTO}
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "字典DTO创建DTO")
@AutoMapper(target = Dict.class)
public record DictCreateDTO(
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "字典类型", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "字典类型不能为空") @Size(max = 64, message = "字典类型长度不能超过64个字符") String dictType,
        @Schema(description = "字典标签", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "字典标签不能为空") @Size(max = 128, message = "字典标签长度不能超过128个字符") String dictLabel,
        @Schema(description = "字典值", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "字典值不能为空") @Size(max = 128, message = "字典值长度不能超过128个字符") String dictValue
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
