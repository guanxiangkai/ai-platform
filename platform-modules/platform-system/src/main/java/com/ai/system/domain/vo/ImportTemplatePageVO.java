package com.ai.system.domain.vo;

import com.ai.system.domain.entity.ImportTemplate;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 导入模板分页VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "导入模板分页VO")
@AutoMapper(target = ImportTemplate.class)
public class ImportTemplatePageVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

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

    @Schema(description = "目标表名")
    private String targetTable;

    @Schema(description = "是否使用自定义处理器")
    private Boolean customImportEnabled;

    @Schema(description = "自定义处理器键")
    private String handlerKey;
}
