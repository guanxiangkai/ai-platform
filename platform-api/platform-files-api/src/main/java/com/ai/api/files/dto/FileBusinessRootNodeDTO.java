package com.ai.api.files.dto;

import java.io.Serial;
import java.io.Serializable;

/** 业务根目录范围内可读取的单个节点。 */
public record FileBusinessRootNodeDTO(
        String id,
        String parentId,
        String name,
        String displayPath,
        String type
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
