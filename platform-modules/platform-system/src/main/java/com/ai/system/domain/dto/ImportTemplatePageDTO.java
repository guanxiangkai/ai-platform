package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 导入模板分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "导入模板分页查询参数")
public class ImportTemplatePageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "所属模块")
    private String templateModule;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
