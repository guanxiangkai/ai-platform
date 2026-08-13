package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import com.ai.system.domain.entity.RoleMenu;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色菜单关系Repository
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface RoleMenuRepository extends JpaPlusRepository<RoleMenu, String> {

    /**
     * 根据角色ID查询菜单关系
     *
     * @param roleId 角色ID
     * @return 角色菜单关系列表
     */
    default List<RoleMenu> findByRoleId(String roleId) {
        return findByRoleIdAndDeletedFalse(roleId);
    }

    /**
     * 根据菜单ID查询角色关系
     *
     * @param menuId 菜单ID
     * @return 角色菜单关系列表
     */
    default List<RoleMenu> findByMenuId(String menuId) {
        return findByMenuIdAndDeletedFalse(menuId);
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
     * 根据菜单ID删除关系
     *
     * @param menuId 菜单ID
     */
    default void deleteByMenuId(String menuId) {
        hardDeleteByMenuId(menuId);
    }

    /**
     * 根据角色ID和菜单ID查询关系
     *
     * @param roleId 角色ID
     * @param menuId 菜单ID
     * @return 角色菜单关系
     */
    default RoleMenu findByRoleIdAndMenuId(String roleId, String menuId) {
        return findByRoleIdAndMenuIdAndDeletedFalse(roleId, menuId);
    }

    /**
     * 根据角色ID列表查询菜单关系
     *
     * @param roleIds 角色ID列表
     * @return 角色菜单关系列表
     */
    default List<RoleMenu> findByRoleIdIn(List<String> roleIds) {
        return findByRoleIdInAndDeletedFalse(roleIds);
    }

    List<RoleMenu> findByRoleIdAndDeletedFalse(String roleId);

    List<RoleMenu> findByMenuIdAndDeletedFalse(String menuId);

    RoleMenu findByRoleIdAndMenuIdAndDeletedFalse(String roleId, String menuId);

    List<RoleMenu> findByRoleIdInAndDeletedFalse(List<String> roleIds);

    /**
     * 根据角色ID列表查询所有菜单ID
     *
     * @param roleIds 角色ID列表
     * @return 菜单ID列表
     */
    @Query("SELECT DISTINCT rm.menuId FROM RoleMenu rm WHERE rm.roleId IN :roleIds AND rm.deleted = false")
    List<String> findMenuIdsByRoleIds(@Param("roleIds") List<String> roleIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM sys_role_menu WHERE role_id = :roleId", nativeQuery = true)
    void hardDeleteByRoleId(@Param("roleId") String roleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM sys_role_menu WHERE menu_id = :menuId", nativeQuery = true)
    void hardDeleteByMenuId(@Param("menuId") String menuId);
}
