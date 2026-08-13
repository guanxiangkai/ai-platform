package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.RoleDTO;
import com.ai.system.domain.dto.RolePageDTO;
import com.ai.system.domain.entity.Role;
import com.ai.system.domain.vo.RolePageVO;
import com.ai.system.domain.vo.RoleVO;

import java.util.List;
import java.util.Map;

/**
 * 角色服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IRoleService extends IBaseService<RolePageDTO, RolePageVO, RoleVO, RoleDTO, RoleDTO, Role> {

    /**
     * 获取角色选项列表
     */
    List<OptionItem> options();

    /**
     * 检查角色编码是否可用
     */
    Boolean checkCode(String code);

    /**
     * 获取角色权限列表
     */
    List<String> getRolePermissions(String id);

    /**
     * 分配权限给角色
     */
    Boolean assignPermissions(String id, List<String> permissionIds);

    /**
     * 复制角色
     */
    String duplicate(String id);

    /**
     * 批量获取用户的角色名称（userId → List&lt;roleName&gt;）
     */
    Map<String, List<String>> getRoleNamesByUserIds(List<String> userIds);

    /**
     * 获取指定用户的角色ID列表
     */
    List<String> getUserRoleIds(String userId);

    /**
     * 获取指定用户的角色编码列表
     */
    List<String> getUserRoleCodes(String userId);

    /**
     * 为用户重新分配角色（先删后插）
     */
    void assignRolesToUser(String userId, List<String> roleIds);
}
