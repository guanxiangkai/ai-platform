package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.util.List;

/**
 * 导入模板实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "sys_import_template",
        comment = "导入模板表",
        check = {
                @CheckConstraint(name = "chk_import_template_write_mode",
                        constraint = "write_mode IN ('INSERT', 'UPSERT')"),
                @CheckConstraint(name = "chk_import_template_header_row",
                        constraint = "header_row_index >= 0"),
                @CheckConstraint(name = "chk_import_template_batch_size",
                        constraint = "batch_size BETWEEN 1 AND 5000")
        }
)
public class ImportTemplate extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "template_module", nullable = false, length = 64, comment = "所属模块")
    private String templateModule;

    @Column(name = "template_code", nullable = false, length = 64, comment = "模板编码")
    private String templateCode;

    @Column(name = "template_name", nullable = false, length = 128, comment = "模板名称")
    private String templateName;

    @Column(name = "file_type", nullable = false, length = 32, comment = "文件类型")
    private String fileType;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "file_name_patterns", columnDefinition = "text[]", comment = "可匹配的文件名模式")
    private List<String> fileNamePatterns = List.of();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "sheet_names", columnDefinition = "text[]", comment = "可导入的Sheet名称")
    private List<String> sheetNames = List.of();

    @Column(name = "target_schema", length = 64, comment = "目标Schema")
    private String targetSchema;

    @Column(name = "target_table", length = 128, comment = "目标表名")
    private String targetTable;

    @Column(name = "custom_import_enabled", nullable = false, columnDefinition = "boolean default true", comment = "是否使用自定义导入处理器")
    private Boolean customImportEnabled = true;

    @Column(name = "handler_key", length = 64, comment = "自定义导入处理器键")
    private String handlerKey;

    @Column(name = "write_mode", nullable = false, length = 16, columnDefinition = "varchar(16) default 'INSERT'", comment = "写入方式")
    private String writeMode = "INSERT";

    @Column(name = "header_row_index", nullable = false, columnDefinition = "integer default 0", comment = "表头行索引")
    private Integer headerRowIndex = 0;

    @Column(name = "batch_size", nullable = false, columnDefinition = "integer default 500", comment = "批处理行数")
    private Integer batchSize = 500;
}
