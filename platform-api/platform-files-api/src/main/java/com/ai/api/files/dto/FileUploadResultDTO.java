package com.ai.api.files.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 平台文件上传结果。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public record FileUploadResultDTO(
        String fileId,
        String versionId,
        String originName,
        String storeName,
        String contentType,
        Long size,
        String url,
        String hash
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
