package com.ai.system.domain.vo;

import com.ai.system.domain.entity.OssLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件上传日志分页列表 VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件上传日志分页VO")
@AutoMapper(target = OssLog.class)
public class OssLogPageVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "上传状态（SUCCESS/FAIL）")
    private String status;

    @Schema(description = "业务模块（AVATAR/KNOWLEDGE/WORK_TICKET等）")
    private String bizModule;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件后缀")
    private String fileSuffix;

    @Schema(description = "文件访问URL")
    private String fileUrl;

    @Schema(description = "上传时间")
    private LocalDateTime logTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
