package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 字典项实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Table(name = "sys_dict_item", comment = "字典项表")
public class DictItem extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典ID
     */
    @Column(name = "dict_id", length = 64, comment = "字典ID")
    private String dictId;

    /**
     * 字典代码
     */
    @Column(name = "dict_code", length = 100, comment = "字典代码")
    private String dictCode;

    /**
     * 字典项值
     */
    @Column(name = "item_value", length = 100, comment = "字典项值")
    private String itemValue;

    /**
     * 字典项标签
     */
    @Column(name = "item_label", length = 100, comment = "字典项标签")
    private String itemLabel;

    /**
     * 样式类型
     */
    @Column(name = "item_style", length = 50, comment = "样式类型")
    private String itemStyle;

    /**
     * 字典颜色
     */
    @Column(name = "item_color", length = 20, comment = "字典颜色")
    private String itemColor;

    /**
     * CSS样式类名
     */
    @Column(name = "item_css_class", length = 128, comment = "CSS样式类名")
    private String itemCssClass;

    /**
     * 是否默认选中
     */
    @Column(name = "item_selected", nullable = false, columnDefinition = "boolean default false", comment = "是否默认选中")
    private Boolean itemSelected = false;

}
