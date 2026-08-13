package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 导入模板字段映射分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "导入模板字段映射分页查询参数")
public class ImportTemplateFieldPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "标题（模糊查询）")
    private String title;

    @Schema(description = "目标字段（模糊查询）")
    private String field;

    @Schema(description = "是否完全匹配")
    private Boolean exactMatch;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "是否必填字段")
    private Boolean required;

    @Schema(description = "是否允许重复")
    private Boolean repeat;

    @Schema(description = "是否多值字段")
    private Boolean multiple;
}
