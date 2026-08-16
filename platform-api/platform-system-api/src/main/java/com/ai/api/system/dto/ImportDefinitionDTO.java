package com.ai.api.system.dto;

import java.io.Serializable;
import java.util.List;

/** platform-system 对外提供的完整启用导入定义。 */
public record ImportDefinitionDTO(
        String code,
        String name,
        List<String> fileNamePatterns,
        List<String> fileExtensions,
        List<String> sheetNames,
        String targetSchema,
        String targetTable,
        boolean customImportEnabled,
        String handlerKey,
        String writeMode,
        int headerRowIndex,
        Integer batchSize,
        List<ImportDefinitionFieldDTO> fields
) implements Serializable {
}
