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
 * 字典实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_dict", comment = "字典表")
public class Dict extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "dict_type", nullable = false, length = 64, comment = "字典类型")
    private String dictType;

    @Column(name = "dict_label", nullable = false, length = 128, comment = "字典标签")
    private String dictLabel;

    @Column(name = "dict_value", nullable = false, length = 128, comment = "字典值")
    private String dictValue;
}
