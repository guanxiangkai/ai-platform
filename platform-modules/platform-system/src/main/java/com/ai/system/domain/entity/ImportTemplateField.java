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
 * 导入模板字段映射实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "sys_import_template_field",
        comment = "导入模板字段映射表",
        check = @CheckConstraint(
                name = "chk_import_template_field_data_type",
                constraint = "data_type IN ('STRING', 'INTEGER', 'LONG', 'DECIMAL', 'BOOLEAN', "
                        + "'LOCAL_DATE', 'LOCAL_DATE_TIME', 'UUID', 'JSON')"
        )
)
public class ImportTemplateField extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "template_id", nullable = false, length = 64, comment = "模板ID")
    private String templateId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "field_title", nullable = false, columnDefinition = "text[]", comment = "Excel列标题数组")
    private List<String> title = List.of();

    @Column(name = "field", nullable = false, length = 128, comment = "目标字段")
    private String field;

    @Column(name = "target_column", length = 128, comment = "数据库目标列")
    private String targetColumn;

    @Column(name = "data_type", nullable = false, length = 32, columnDefinition = "varchar(32) default 'STRING'", comment = "数据类型")
    private String dataType = "STRING";

    @Column(name = "format_pattern", length = 128, comment = "日期时间等格式")
    private String formatPattern;

    @Column(name = "default_value", length = 512, comment = "缺省值")
    private String defaultValue;

    @Column(name = "converter_key", length = 64, comment = "自定义转换器键")
    private String converterKey;

    @Column(name = "exact_match", nullable = false, columnDefinition = "boolean default false", comment = "是否完全匹配")
    private Boolean exactMatch = false;

    @Column(name = "required", nullable = false, columnDefinition = "boolean default false", comment = "是否必填字段")
    private Boolean required = false;

    @Column(name = "multiple", nullable = false, columnDefinition = "boolean default false", comment = "是否多值字段")
    private Boolean multiple = false;

    @Column(name = "repeat", nullable = false, columnDefinition = "boolean default true", comment = "是否允许重复")
    private Boolean repeat = true;

}
