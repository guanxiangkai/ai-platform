package com.ai.files.repository;

import com.ai.files.domain.FileVersionState;
import com.ai.files.domain.entity.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文件版本持久化接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, String> {

    /** 返回节点已分配的最大版本号；调用方必须先锁定节点。 */
    @Query("select max(version.versionNo) from FileVersion version "
            + "where version.nodeId = :nodeId and version.tenantId = :tenantId "
            + "and version.deleted = false")
    Optional<Integer> findMaxVersionNo(
            @Param("nodeId") String nodeId, @Param("tenantId") String tenantId);

    /** 查询文件节点的全部可用版本。 */
    List<FileVersion> findByNodeIdAndVersionStateAndDeletedFalseOrderByVersionNoDesc(
            String nodeId, FileVersionState versionState);

    /** 按租户边界查询未删除版本。 */
    Optional<FileVersion> findByIdAndTenantIdAndDeletedFalse(String id, String tenantId);

    /** 查询当前租户指定节点的可下载版本。 */
    Optional<FileVersion> findByIdAndNodeIdAndTenantIdAndVersionStateAndDeletedFalse(
            String id, String nodeId, String tenantId, FileVersionState versionState);
}
