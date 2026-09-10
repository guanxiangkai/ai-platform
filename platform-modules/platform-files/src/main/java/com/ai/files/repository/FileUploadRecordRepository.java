package com.ai.files.repository;

import com.ai.files.domain.entity.FileUploadRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 文件上传 saga 持久化接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface FileUploadRecordRepository extends JpaRepository<FileUploadRecord, String> {

    /** 锁定当前租户的上传记录以推进状态机。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select upload from FileUploadRecord upload where upload.id = :id "
            + "and upload.tenantId = :tenantId and upload.deleted = false")
    Optional<FileUploadRecord> findLockedByIdAndTenantId(
            @Param("id") String id, @Param("tenantId") String tenantId);
    /** 读取准确版本的实际上传者与业务关联，供原子封存校验。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from FileUploadRecord u where u.tenantId=:tenant and u.nodeId=:node and u.versionId=:version and u.deleted=false")
    Optional<FileUploadRecord> findLockedVersion(@Param("tenant") String tenant,@Param("node") String node,@Param("version") String version);
}
