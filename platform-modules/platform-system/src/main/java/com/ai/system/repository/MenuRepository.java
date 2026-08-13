package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.query.wrapper.QueryWrapper;
import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.Menu;
import com.ai.system.domain.vo.MenuPageVO;
import com.ai.system.domain.vo.MenuVO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 菜单数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface MenuRepository
        extends BaseRepository<MenuPageVO, MenuVO, Menu>,
                JpaPlusRepository<Menu, String> {

    /** 根据父菜单ID查询子菜单（过滤逻辑删除） */
    default List<Menu> findByParentIdAndDeletedFalse(String parentId) {
        return list(QueryWrapper.from(Menu.class)
                .eq(Menu::getParentId, parentId)
                .eq(Menu::getDeleted, false));
    }

    /** 根据 ID 查询菜单（过滤逻辑删除） */
    default Optional<Menu> findByIdAndDeletedFalse(String id) {
        return one(QueryWrapper.from(Menu.class)
                .eq(Menu::getId, id)
                .eq(Menu::getDeleted, false));
    }

    /** 查询所有未删除菜单 */
    default List<Menu> findByDeletedFalse() {
        return list(QueryWrapper.from(Menu.class)
                .eq(Menu::getDeleted, false));
    }

    /** 查询所有启用的菜单（过滤逻辑删除） */
    default List<Menu> findByEnabledTrueAndDeletedFalse() {
        return list(QueryWrapper.from(Menu.class)
                .eq(Menu::getEnabled, true)
                .eq(Menu::getDeleted, false));
    }

    /** 检查有效菜单路径是否已存在。 */
    default boolean existsByPathAndDeletedFalse(String path) {
        return count(QueryWrapper.from(Menu.class)
                .eq(Menu::getPath, path)
                .eq(Menu::getDeleted, false)) > 0;
    }

    /** 查询全部有效权限标识，供超级管理员权限缓存刷新使用 */
    @Query("""
            SELECT DISTINCT m.permission
            FROM Menu m
            WHERE m.deleted = false
              AND m.permission IS NOT NULL
              AND m.permission <> ''
            """)
    List<String> findAllPermissions();

    /** 根据菜单 ID 查询有效权限标识，避免加载完整菜单实体 */
    @Query("""
            SELECT DISTINCT m.permission
            FROM Menu m
            WHERE m.id IN :ids
              AND m.deleted = false
              AND m.permission IS NOT NULL
              AND m.permission <> ''
            """)
    List<String> findPermissionsByIds(@Param("ids") Collection<String> ids);
}
