package com.ai.system.domain.dto;

import com.ai.system.domain.entity.ImportTemplateField;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 导入模板字段映射数据传输对象。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "导入模板字段映射DTO")
@AutoMapper(target = ImportTemplateField.class)
public record ImportTemplateFieldDTO(
        @Schema(description = "主键ID（更新时必填）") String id,
        @Schema(description = "备注", maxLength = 500) @Size(max = 500, message = "备注长度不能超过500个字符") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "模板ID", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 64) @NotBlank(message = "模板ID不能为空") @Size(max = 64, message = "模板ID长度不能超过64个字符") String templateId,
        @Schema(description = "Excel列标题数组", requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty(message = "Excel列标题不能为空") @Size(max = 20, message = "Excel列标题最多20个") List<@NotBlank(message = "Excel列标题不能为空") @Size(max = 128, message = "Excel列标题长度不能超过128个字符") String> title,
        @Schema(description = "目标字段", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 128) @NotBlank(message = "字段不能为空") @Size(max = 128, message = "字段长度不能超过128个字符") String field,
        @Schema(description = "数据库目标列", maxLength = 128) @Size(max = 128, message = "数据库目标列长度不能超过128个字符") String targetColumn,
        @Schema(description = "数据类型", allowableValues = {"STRING", "INTEGER", "LONG", "DECIMAL", "BOOLEAN", "LOCAL_DATE", "LOCAL_DATE_TIME", "UUID", "JSON"}) @Pattern(regexp = "STRING|INTEGER|LONG|DECIMAL|BOOLEAN|LOCAL_DATE|LOCAL_DATE_TIME|UUID|JSON", message = "数据类型不支持") String dataType,
        @Schema(description = "格式", maxLength = 128) @Size(max = 128, message = "格式长度不能超过128个字符") String formatPattern,
        @Schema(description = "缺省值", maxLength = 512) @Size(max = 512, message = "缺省值长度不能超过512个字符") String defaultValue,
        @Schema(description = "自定义转换器键", maxLength = 64) @Size(max = 64, message = "转换器键长度不能超过64个字符") String converterKey,
        @Schema(description = "是否完全匹配", requiredMode = Schema.RequiredMode.REQUIRED) Boolean exactMatch,
        @Schema(description = "是否必填字段", requiredMode = Schema.RequiredMode.REQUIRED) Boolean required,
        @Schema(description = "是否多值字段", requiredMode = Schema.RequiredMode.REQUIRED) Boolean multiple,
        @Schema(description = "是否允许重复", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "是否允许重复不能为空") Boolean repeat
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
