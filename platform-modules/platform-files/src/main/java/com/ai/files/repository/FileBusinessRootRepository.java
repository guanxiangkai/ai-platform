package com.ai.files.repository;

import com.ai.files.domain.entity.FileBusinessRoot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 业务文件根目录绑定持久化接口。 */
@Repository
public interface FileBusinessRootRepository extends JpaRepository<FileBusinessRoot, String> {

    /** 按当前租户业务记录查询根目录绑定。 */
    Optional<FileBusinessRoot> findByTenantIdAndBusinessTypeAndBusinessIdAndDeletedFalse(
            String tenantId, String businessType, String businessId);

    /** 按当前租户和根节点查询业务根目录绑定。 */
    Optional<FileBusinessRoot> findByTenantIdAndRootNodeIdAndDeletedFalse(String tenantId, String rootNodeId);

    /** 判断节点是否为活动业务根目录，避免通用回收误处理根目录。 */
    boolean existsByRootNodeIdAndDeletedFalse(String rootNodeId);
}
