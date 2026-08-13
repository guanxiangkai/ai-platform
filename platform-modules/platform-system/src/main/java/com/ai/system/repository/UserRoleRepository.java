package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import com.ai.system.domain.entity.UserRole;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色关系Repository
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface UserRoleRepository extends JpaPlusRepository<UserRole, String> {

    /**
     * 根据用户ID查询角色关系
     *
     * @param userId 用户ID
     * @return 用户角色关系列表
     */
    default List<UserRole> findByUserId(String userId) {
        return findByUserIdAndDeletedFalse(userId);
    }

    /**
     * 根据角色ID查询用户关系
     *
     * @param roleId 角色ID
     * @return 用户角色关系列表
     */
    default List<UserRole> findByRoleId(String roleId) {
        return findByRoleIdAndDeletedFalse(roleId);
    }

    /**
     * 根据用户ID删除关系
     *
     * @param userId 用户ID
     */
    default void deleteByUserId(String userId) {
        hardDeleteByUserId(userId);
    }

    /**
     * 根据角色ID删除关系
     *
     * @param roleId 角色ID
     */
    default void deleteByRoleId(String roleId) {
        hardDeleteByRoleId(roleId);
    }

    /**
     * 根据用户ID和角色ID查询关系
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 用户角色关系
     */
    default UserRole findByUserIdAndRoleId(String userId, String roleId) {
        return findByUserIdAndRoleIdAndDeletedFalse(userId, roleId);
    }

    /**
     * 根据用户ID列表查询角色关系
     *
     * @param userIds 用户ID列表
     * @return 用户角色关系列表
     */
    default List<UserRole> findByUserIdIn(List<String> userIds) {
        return findByUserIdInAndDeletedFalse(userIds);
    }

    List<UserRole> findByUserIdAndDeletedFalse(String userId);

    List<UserRole> findByRoleIdAndDeletedFalse(String roleId);

    UserRole findByUserIdAndRoleIdAndDeletedFalse(String userId, String roleId);

    List<UserRole> findByUserIdInAndDeletedFalse(List<String> userIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM sys_user_role WHERE user_id = :userId", nativeQuery = true)
    void hardDeleteByUserId(@Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM sys_user_role WHERE role_id = :roleId", nativeQuery = true)
    void hardDeleteByRoleId(@Param("roleId") String roleId);
}
