package com.ai.api.system.dto;

import java.io.Serializable;
import java.util.List;

/** Complete enabled import definition exposed by ai-system. */
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
