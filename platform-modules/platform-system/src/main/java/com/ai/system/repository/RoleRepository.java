package com.ai.system.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.Role;
import com.ai.system.domain.vo.RolePageVO;
import com.ai.system.domain.vo.RoleVO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface RoleRepository extends BaseRepository<RolePageVO, RoleVO, Role> {

    /**
     * 根据角色编码查询角色（过滤逻辑删除）
     *
     * @param code 角色编码
     * @return 角色实体
     */
    Optional<Role> findByRoleCodeAndDeletedFalse(String roleCode);

    /** 查询可供选择的启用角色（过滤逻辑删除）。 */
    List<Role> findByEnabledTrueAndDeletedFalse(Pageable pageable);

    /**
     * 检查角色编码是否存在（过滤逻辑删除）
     *
     * @param code 角色编码
     * @return 是否存在
     */
    boolean existsByRoleCodeAndDeletedFalse(String roleCode);

    /**
     * 根据角色名称精确查询（过滤逻辑删除）
     * <p>用于注册审核时按"部门名称+职位名称"或仅"职位名称"匹配角色</p>
     */
    Optional<Role> findFirstByRoleNameAndDeletedFalse(String roleName);

    Optional<Role> findFirstByRoleNameAndEnabledTrueAndDeletedFalse(String roleName);

    /**
     * 查询默认注册角色（注册审核兜底角色）
     */
    Optional<Role> findFirstByDefaultRegistrationRoleTrueAndDeletedFalse();

    Optional<Role> findFirstByDefaultRegistrationRoleTrueAndEnabledTrueAndDeletedFalse();
}
