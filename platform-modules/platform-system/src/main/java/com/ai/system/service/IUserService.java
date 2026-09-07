package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.system.domain.dto.UserCreateDTO;
import com.ai.system.domain.dto.UserDTO;
import com.ai.system.domain.dto.UserPageDTO;
import com.ai.system.domain.entity.User;
import com.ai.system.domain.vo.UserPageVO;
import com.ai.system.domain.vo.UserVO;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IUserService extends IBaseService<UserPageDTO, UserPageVO, UserVO, UserCreateDTO, UserDTO, User> {

    /**
     * 修改用户密码
     *
     * @param id          用户ID
     * @param oldPassword 旧密码的 SHA-1 小写十六进制摘要
     * @param newPassword 新密码的 SHA-1 小写十六进制摘要
     * @return 是否成功
     */
    Boolean changePassword(String id, String oldPassword, String newPassword);

    /**
     * 重置用户密码
     *
     * @param id 用户ID
     * @param newPassword 管理端本地生成新密码的 SHA-1 小写十六进制摘要
     * @return 是否重置成功
     */
    Boolean resetPassword(String id, String newPassword);

    /**
     * 检查用户名是否可用
     *
     * @param username 用户名
     * @return 是否可用
     */
    Boolean checkUsername(String username);

    /**
     * 获取用户角色
     *
     * @param id 用户ID
     * @return 角色ID列表
     */
    List<String> getUserRoles(String id);

    /**
     * 分配用户角色
     *
     * @param id      用户ID
     * @param roleIds 角色ID列表
     * @param userType 用户类型
     * @return 是否成功
     */
    Boolean assignRoles(String id, List<String> roleIds, String userType);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    UserVO findByUsername(String username);

    /**
     * 根据用户名判断是否存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    Boolean existsByEmail(String email);

}
