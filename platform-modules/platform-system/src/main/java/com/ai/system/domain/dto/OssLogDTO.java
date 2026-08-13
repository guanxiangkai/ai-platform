package com.ai.system.domain.dto;

import com.ai.system.domain.entity.OssLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件上传日志 DTO（日志由 SPI 自动写入，此 DTO 仅用于 BaseController/IBaseService 泛型约束）
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "文件上传日志DTO")
@AutoMapper(target = OssLog.class)
public record OssLogDTO(
        @Schema(description = "原始文件名") String originalName
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
