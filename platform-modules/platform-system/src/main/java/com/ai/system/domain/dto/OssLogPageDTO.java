package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 文件上传日志分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件上传日志分页查询参数")
public class OssLogPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名（模糊查询）")
    private String username;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "上传状态（SUCCESS/FAIL）")
    private String status;

    @Schema(description = "业务模块（AVATAR/KNOWLEDGE/WORK_TICKET/DOCUMENT/TEMP等）")
    private String bizModule;

    @Schema(description = "文件名（模糊查询）")
    private String originalName;

    @Schema(description = "文件后缀")
    private String fileSuffix;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
