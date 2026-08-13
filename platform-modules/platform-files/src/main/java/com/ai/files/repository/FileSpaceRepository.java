package com.ai.files.repository;

import com.ai.files.domain.FilePrincipalType;
import com.ai.files.domain.FileSpaceType;
import com.ai.files.domain.entity.FileSpace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 文件空间持久化接口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface FileSpaceRepository extends JpaRepository<FileSpace, String> {

    /** 按空间编码查询未删除空间。 */
    Optional<FileSpace> findBySpaceCodeAndDeletedFalse(String spaceCode);

    /** 查询个人空间。 */
    Optional<FileSpace> findBySpaceTypeAndOwnerUserIdAndDeletedFalse(FileSpaceType type, String ownerUserId);

    /** 查询部门空间。 */
    Optional<FileSpace> findBySpaceTypeAndOwnerDeptIdAndDeletedFalse(FileSpaceType type, String ownerDeptId);

    /** 查询某类空间。 */
    List<FileSpace> findBySpaceTypeAndDeletedFalseOrderBySpaceNameAsc(FileSpaceType type);

    /**
     * 查询当前主体可进入的空间。
     *
     * <p>仅空间级、未过期的授权可形成空间入口；节点级授权仅用于节点访问，不能扩展为整个空间的可见性。
     * 超级管理员可查看当前租户内全部空间，可信内部服务额外可进入系统空间。</p>
     *
     * @param userId 当前用户标识
     * @param departmentIds 当前用户所属部门；没有部门时传入占位集合并将 {@code hasDepartments} 设为 {@code false}
     * @param hasDepartments 是否存在可参与授权和所有权判断的部门
     * @param now 当前时刻，用于排除已过期授权
     * @param superAdmin 是否为超级管理员
     * @param internalService 是否为可信内部服务主体
     * @return 按名称排序的可访问空间
     */
    @Query("""
            select distinct space
            from FileSpace space
            where space.deleted = false
              and (
                    :superAdmin = true
                    or space.ownerUserId = :userId
                    or (:hasDepartments = true
                        and space.spaceType = :departmentSpaceType
                        and space.ownerDeptId in :departmentIds)
                    or (:internalService = true and space.spaceType = :systemSpaceType)
                    or exists (
                        select grant.id
                        from FileGrant grant
                        where grant.spaceId = space.id
                          and grant.tenantId = space.tenantId
                          and grant.deleted = false
                          and grant.nodeId is null
                          and (grant.expiresAt is null or grant.expiresAt > :now)
                          and (
                                (grant.principalType = :userPrincipalType and grant.principalId = :userId)
                                or (:hasDepartments = true
                                    and grant.principalType = :departmentPrincipalType
                                    and grant.principalId in :departmentIds)
                          )
                    )
              )
            order by space.spaceName asc
            """)
    List<FileSpace> findAccessibleSpaces(
            @Param("userId") String userId,
            @Param("departmentIds") List<String> departmentIds,
            @Param("hasDepartments") boolean hasDepartments,
            @Param("now") Instant now,
            @Param("superAdmin") boolean superAdmin,
            @Param("internalService") boolean internalService,
            @Param("departmentSpaceType") FileSpaceType departmentSpaceType,
            @Param("systemSpaceType") FileSpaceType systemSpaceType,
            @Param("userPrincipalType") FilePrincipalType userPrincipalType,
            @Param("departmentPrincipalType") FilePrincipalType departmentPrincipalType);

    /** 锁定空间配额边界，用于原子预留上传容量。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select space from FileSpace space where space.id = :id "
            + "and space.tenantId = :tenantId and space.deleted = false")
    Optional<FileSpace> findLockedByIdAndTenantId(
            @Param("id") String id, @Param("tenantId") String tenantId);
}
