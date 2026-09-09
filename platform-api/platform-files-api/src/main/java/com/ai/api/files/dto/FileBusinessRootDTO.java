package com.ai.api.files.dto;

import java.io.Serial;
import java.io.Serializable;

/** 当前租户一个业务记录绑定的稳定文件根目录。 */
public record FileBusinessRootDTO(
        String rootId,
        String spaceId,
        String displayPath,
        String displayName
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
