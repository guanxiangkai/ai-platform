package com.ai.files.repository;

import com.ai.files.domain.FileNodeState;
import com.ai.files.domain.FileNodeType;
import com.ai.files.domain.entity.FileNode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 文件节点持久化接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface FileNodeRepository extends JpaRepository<FileNode, String> {

    /** 查询未删除节点。 */
    Optional<FileNode> findByIdAndDeletedFalse(String id);

    /** 查询目录下的节点。 */
    List<FileNode> findBySpaceIdAndParentIdAndNodeStateAndDeletedFalseOrderByNodeNameAsc(
            String spaceId, String parentId, FileNodeState state);

    /** 分页查询目录中的活动节点。 */
    Page<FileNode> findBySpaceIdAndParentIdAndNodeStateAndDeletedFalseOrderByNodeNameAsc(
            String spaceId, String parentId, FileNodeState state, Pageable pageable);

    /** 分页查询目录中指定类型的活动节点。 */
    Page<FileNode> findBySpaceIdAndParentIdAndNodeTypeAndNodeStateAndDeletedFalseOrderByNodeNameAsc(
            String spaceId, String parentId, FileNodeType type, FileNodeState state, Pageable pageable);

    /** 查询同目录同名活动节点。 */
    Optional<FileNode> findBySpaceIdAndParentIdAndNodeNameAndNodeStateAndDeletedFalse(
            String spaceId, String parentId, String nodeName, FileNodeState state);

    /** 查询路径下全部节点，用于移动、回收与恢复时同步后代。 */
    List<FileNode> findBySpaceIdAndDisplayPathStartingWithAndDeletedFalseOrderByDisplayPathAsc(
            String spaceId, String displayPathPrefix);

    /** 按租户和业务标识查询活动业务文件。 */
    List<FileNode> findByTenantIdAndBusinessTypeAndBusinessIdAndNodeTypeAndNodeStateAndDeletedFalseOrderByCreateTimeDesc(
            String tenantId, String businessType, String businessId, FileNodeType nodeType, FileNodeState state);

    /** 查询同目录中占用名称的可见或上传中节点。 */
    Optional<FileNode> findBySpaceIdAndParentIdAndNodeNameAndNodeStateInAndDeletedFalse(
            String spaceId, String parentId, String nodeName, Set<FileNodeState> states);

    /** 锁定文件节点的版本分配边界。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select node from FileNode node where node.id = :id "
            + "and node.tenantId = :tenantId and node.deleted = false")
    Optional<FileNode> findLockedByIdAndTenantId(
            @Param("id") String id, @Param("tenantId") String tenantId);
}
