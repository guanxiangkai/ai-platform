package com.ai.system.domain.vo;

import com.ai.system.domain.entity.ImportTemplate;
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
 * 导入模板VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "导入模板VO")
@AutoMapper(target = ImportTemplate.class)
public class ImportTemplateVO implements Serializable {
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

    @Schema(description = "所属模块")
    private String templateModule;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "可匹配的文件名模式")
    private List<String> fileNamePatterns;

    @Schema(description = "可导入的Sheet名称")
    private List<String> sheetNames;

    @Schema(description = "目标Schema")
    private String targetSchema;

    @Schema(description = "目标表名")
    private String targetTable;

    @Schema(description = "是否使用自定义处理器")
    private Boolean customImportEnabled;

    @Schema(description = "自定义处理器键")
    private String handlerKey;

    @Schema(description = "写入方式")
    private String writeMode;

    @Schema(description = "表头行索引")
    private Integer headerRowIndex;

    @Schema(description = "批处理行数")
    private Integer batchSize;
}
