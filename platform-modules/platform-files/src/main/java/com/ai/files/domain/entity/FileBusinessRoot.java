package com.ai.files.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/** 当前租户一个业务记录与系统空间根文件夹的一对一绑定。 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "file_business_root", comment = "业务文件根目录绑定表",
        uniqueConstraints = {@UniqueConstraint(name = "uk_file_business_root_tenant_business",
                columnNames = {"tenant_id", "business_type", "business_id"}),
                @UniqueConstraint(name = "uk_file_business_root_node", columnNames = {"root_node_id"})})
public class FileBusinessRoot extends DataTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务类型。 */
    @Column(name = "business_type", nullable = false, length = 64, comment = "业务类型")
    private String businessType;

    /** 业务记录标识。 */
    @Column(name = "business_id", nullable = false, length = 128, comment = "业务记录标识")
    private String businessId;

    /** 关联的系统空间根文件夹标识。 */
    @Column(name = "root_node_id", nullable = false, length = 64, comment = "根文件夹标识")
    private String rootNodeId;
}
