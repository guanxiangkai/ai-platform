package com.ai.api.files.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 业务根目录单层节点的有界分页结果。 */
public record FileBusinessRootNodePageDTO(
        List<FileBusinessRootNodeDTO> records,
        long total,
        int page,
        int size,
        boolean hasMore
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
