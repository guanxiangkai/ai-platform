package com.ai.api.files.dto;

import java.io.Serial;
import java.io.Serializable;

/** 上传到业务根目录后的文件位置及本次实际版本。 */
public record FileBusinessUploadDTO(
        FileUploadResultDTO file,
        String rootId,
        String parentId,
        String relativePath,
        String displayPath
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
