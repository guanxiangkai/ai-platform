package com.ai.api.system.dto;

import java.io.Serializable;
import java.util.List;

/** Configuration-driven import field contract shared between system and business services. */
public record ImportDefinitionFieldDTO(
        String field,
        List<String> sourceHeaders,
        String targetColumn,
        String dataType,
        String formatPattern,
        String defaultValue,
        String converterKey,
        boolean exactMatch,
        boolean required,
        boolean multiple,
        boolean repeat,
        int order
) implements Serializable {
}
