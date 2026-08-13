package com.ai.system.domain.dto;

import com.ai.system.domain.entity.ImportTemplate;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 导入模板创建DTO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "导入模板创建DTO")
@AutoMapper(target = ImportTemplate.class)
public record ImportTemplateCreateDTO(
        @Schema(description = "备注", maxLength = 500) @Size(max = 500, message = "备注长度不能超过500个字符") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "所属模块", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 64) @NotBlank(message = "模块不能为空") @Size(max = 64, message = "模块长度不能超过64个字符") String templateModule,
        @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 64) @NotBlank(message = "模板编码不能为空") @Size(max = 64, message = "模板编码长度不能超过64个字符") String templateCode,
        @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 128) @NotBlank(message = "模板名称不能为空") @Size(max = 128, message = "模板名称长度不能超过128个字符") String templateName,
        @Schema(description = "文件类型", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 32) @NotBlank(message = "文件类型不能为空") @Pattern(regexp = "(?i)xlsx|xls|csv", message = "文件类型仅支持xlsx、xls或csv") String fileType,
        @Schema(description = "可匹配的文件名模式") @Size(max = 20, message = "文件名模式最多20个") List<@Size(max = 256, message = "文件名模式长度不能超过256个字符") String> fileNamePatterns,
        @Schema(description = "可导入的Sheet名称") @Size(max = 20, message = "Sheet名称最多20个") List<@Size(max = 64, message = "Sheet名称长度不能超过64个字符") String> sheetNames,
        @Schema(description = "目标Schema", maxLength = 64) @Size(max = 64, message = "目标Schema长度不能超过64个字符") String targetSchema,
        @Schema(description = "目标表名", maxLength = 128) @Size(max = 128, message = "目标表名长度不能超过128个字符") String targetTable,
        @Schema(description = "是否使用自定义处理器") Boolean customImportEnabled,
        @Schema(description = "自定义处理器键", maxLength = 64) @Size(max = 64, message = "处理器键长度不能超过64个字符") String handlerKey,
        @Schema(description = "写入方式", allowableValues = {"INSERT", "UPSERT"}) @Pattern(regexp = "INSERT|UPSERT", message = "写入方式仅支持INSERT或UPSERT") String writeMode,
        @Schema(description = "表头行索引", minimum = "0", maximum = "100") @Min(value = 0, message = "表头行索引不能小于0") @Max(value = 100, message = "表头行索引不能大于100") Integer headerRowIndex,
        @Schema(description = "批处理行数", minimum = "1", maximum = "5000") @Min(value = 1, message = "批处理行数不能小于1") @Max(value = 5000, message = "批处理行数不能大于5000") Integer batchSize
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
