package com.ai.system.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.User;
import com.ai.system.domain.vo.UserPageVO;
import com.ai.system.domain.vo.UserVO;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends BaseRepository<UserPageVO, UserVO, User> {

    /** 批量查询当前租户内未删除的用户。 */
    List<User> findAllByIdInAndDeletedFalse(Collection<String> ids);

    /** 批量查询当前租户内未删除的用户。 */
    List<User> findAllByUsernameInAndDeletedFalse(Collection<String> usernames);

    /**
     * 根据用户名查询用户（过滤逻辑删除）
     */
    Optional<User> findByUsernameAndDeletedFalse(String username);

    /**
     * 根据邮箱查询用户（过滤逻辑删除）
     */
    Optional<User> findByEmailAndDeletedFalse(String email);

    /**
     * 根据手机号查询用户（过滤逻辑删除）
     */
    Optional<User> findByPhoneAndDeletedFalse(String phone);

    /**
     * 检查用户名是否存在（过滤逻辑删除）
     */
    boolean existsByUsernameAndDeletedFalse(String username);

    /**
     * 检查邮箱是否存在（过滤逻辑删除）
     */
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * 检查手机号是否存在（过滤逻辑删除）
     */
    boolean existsByPhoneAndDeletedFalse(String phone);

}
