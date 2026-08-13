package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.system.domain.dto.MenuCreateDTO;
import com.ai.system.domain.dto.MenuDTO;
import com.ai.system.domain.dto.MenuPageDTO;
import com.ai.system.domain.entity.Menu;
import com.ai.system.domain.vo.MenuPageVO;
import com.ai.system.domain.vo.MenuVO;

import java.util.List;
import java.util.Set;

/**
 * 菜单服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IMenuService extends IBaseService<MenuPageDTO, MenuPageVO, MenuVO, MenuCreateDTO, MenuDTO, Menu> {

    /**
     * 获取菜单树
     *
     * @return 菜单树列表
     */
    List<MenuVO> tree();

    /**
     * 查询当前可分配给角色的有效菜单。
     *
     * @return 启用且未删除的菜单列表，包含按钮权限节点
     */
    List<MenuVO> getAssignableMenus();

    /**
     * 查询当前可分配给角色的有效菜单树。
     *
     * @return 启用且未删除的菜单树，包含按钮权限节点
     */
    List<MenuVO> getAssignableMenuTree();

    /**
     * 获取用户菜单
     *
     * @return 用户菜单列表
     */
    List<MenuVO> getUserMenus();

    /**
     * 更新菜单排序
     *
     * @param ids 排序后的菜单ID列表
     * @return 是否更新成功
     */
    Boolean updateSort(List<String> ids);

    /**
     * 检查菜单路径是否可用
     *
     * @param path 菜单路径
     * @return 是否可用
     */
    Boolean checkPath(String path);

    /**
     * 获取可用的父级菜单
     *
     * @return 可用的父级菜单列表
     */
    List<MenuVO> getAvailableParents();

    /**
     * 更新菜单可见性
     *
     * @param id      菜单ID
     * @param visible 是否可见
     * @return 是否更新成功
     */
    Boolean updateVisibility(String id, Boolean visible);

    /**
     * 获取用户权限集合
     * <p>
     * 通过用户的角色查询所有菜单权限代码
     *
     * @param userId     用户ID
     * @param superAdmin 是否是超级管理员
     * @return 权限代码集合
     */
    Set<String> getUserPermissions(String userId, Boolean superAdmin);
}
