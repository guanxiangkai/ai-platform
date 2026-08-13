package com.ai.system.domain.vo;

import com.ai.system.domain.entity.ImportTemplateField;
import com.ai.system.constants.SystemConstants.DateTimeConstants;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 导入模板字段映射视图对象
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "导入模板字段映射VO")
@AutoMapper(target = ImportTemplateField.class)
public class ImportTemplateFieldVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @JsonFormat(pattern = DateTimeConstants.DATE_TIME, timezone = DateTimeConstants.TIME_ZONE)
    @Schema(description = "创建时间", pattern = DateTimeConstants.DATE_TIME, example = "2026-07-10 09:30:00")
    private LocalDateTime createTime;

    @JsonFormat(pattern = DateTimeConstants.DATE_TIME, timezone = DateTimeConstants.TIME_ZONE)
    @Schema(description = "更新时间", pattern = DateTimeConstants.DATE_TIME, example = "2026-07-10 09:30:00")
    private LocalDateTime updateTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "Excel列标题数组")
    private List<String> title;

    @Schema(description = "目标字段")
    private String field;

    @Schema(description = "数据库目标列")
    private String targetColumn;

    @Schema(description = "数据类型")
    private String dataType;

    @Schema(description = "格式")
    private String formatPattern;

    @Schema(description = "缺省值")
    private String defaultValue;

    @Schema(description = "自定义转换器键")
    private String converterKey;

    @Schema(description = "是否完全匹配")
    private Boolean exactMatch;

    @Schema(description = "是否必填字段")
    private Boolean required;

    @Schema(description = "是否多值字段")
    private Boolean multiple;

    @Schema(description = "是否允许重复")
    private Boolean repeat;
}
