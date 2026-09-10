package com.ai.files.repository;

import com.ai.files.domain.entity.FileBrowserUploadTarget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** 单次浏览器上传预约的持久约束。 */
public interface FileBrowserUploadTargetRepository extends JpaRepository<FileBrowserUploadTarget,String> {
    /** 与空间、文件节点锁共同保护一次成功上传及封存。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from FileBrowserUploadTarget t where t.id=:id and t.tenantId=:tenant and t.deleted=false")
    Optional<FileBrowserUploadTarget> findLockedByIdAndTenantId(@Param("id") String id,@Param("tenant") String tenant);
    /** 在空间锁内按业务身份读取稳定目标，目录重命名不能创建另一个目标。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from FileBrowserUploadTarget t where t.tenantId=:tenant and t.businessType=:type and t.businessId=:business and t.deleted=false")
    Optional<FileBrowserUploadTarget> findLockedBusiness(@Param("tenant") String tenant,@Param("type") String type,@Param("business") String business);
}

