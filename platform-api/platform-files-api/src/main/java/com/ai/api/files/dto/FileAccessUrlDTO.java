package com.ai.api.files.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/** 平台文件短时直读地址。 */
public record FileAccessUrlDTO(
        String url,
        Instant expiresAt
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
