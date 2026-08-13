package com.ai.api.files.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前租户内业务记录关联的活动文件。
 *
 * @param fileId 文件节点标识
 * @param filename 文件名称
 * @author guanxiangkai
 * @since 1.0.0
 */
public record FileBusinessFileDTO(String fileId, String filename) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
