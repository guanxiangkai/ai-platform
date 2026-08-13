package com.ai.files.repository;

import com.ai.files.domain.FilePrincipalType;
import com.ai.files.domain.entity.FileGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 文件授权持久化接口。 */
@Repository
public interface FileGrantRepository extends JpaRepository<FileGrant, String> {

    /** 查询某用户或部门在空间内的授权。 */
    List<FileGrant> findBySpaceIdAndPrincipalTypeAndPrincipalIdInAndDeletedFalse(
            String spaceId, FilePrincipalType principalType, Collection<String> principalIds);

    /** 查询当前用户或部门收到的全部授权，用于“共享给我”入口。 */
    List<FileGrant> findByPrincipalTypeAndPrincipalIdInAndDeletedFalse(
            FilePrincipalType principalType, Collection<String> principalIds);

    /** 查询空间或节点的授权清单。 */
    List<FileGrant> findBySpaceIdAndNodeIdAndDeletedFalseOrderByCreateTimeAsc(String spaceId, String nodeId);

    /** 查询同一授权主体的现有记录。 */
    Optional<FileGrant> findBySpaceIdAndNodeIdAndPrincipalTypeAndPrincipalIdAndDeletedFalse(
            String spaceId, String nodeId, FilePrincipalType principalType, String principalId);
}
