package com.ai.files.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import com.ai.files.domain.FileOperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/** 文件创建、下载、共享、编辑和回收等操作的审计记录。 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "file_operation_log", comment = "文件操作审计表")
public class FileOperationLog extends DataTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 操作人用户标识。 */
    @Column(name = "operator_user_id", nullable = false, length = 64, comment = "操作人用户标识")
    private String operatorUserId;

    /** 操作类型。 */
    @Column(name = "operation", nullable = false, length = 32, comment = "文件操作类型")
    @Enumerated(EnumType.STRING)
    private FileOperationType operation;

    /** 关联空间标识。 */
    @Column(name = "space_id", nullable = false, length = 64, comment = "文件空间标识")
    private String spaceId;

    /** 关联节点标识。 */
    @Column(name = "node_id", length = 64, comment = "文件节点标识")
    private String nodeId;

    /** 不含敏感信息的操作摘要。 */
    @Column(name = "detail", length = 1000, comment = "不含敏感信息的操作摘要")
    private String detail;
}
