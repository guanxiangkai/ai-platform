package com.ai.system.service.impl;

import com.ai.system.config.SystemQueryProperties;
import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import com.ai.system.domain.dto.RoleDTO;
import com.ai.system.domain.dto.RolePageDTO;
import com.ai.system.domain.entity.Role;
import com.ai.system.domain.entity.RoleMenu;
import com.ai.system.domain.entity.UserRole;
import com.ai.system.domain.vo.RolePageVO;
import com.ai.system.domain.vo.RoleVO;
import com.ai.system.repository.RoleMenuRepository;
import com.ai.system.repository.RoleRepository;
import com.ai.system.repository.UserRoleRepository;
import com.ai.system.security.AuthUserCacheService;
import com.ai.system.security.AuthorizationCacheService;
import com.ai.system.service.IRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl extends BaseServiceImpl<RolePageDTO, RolePageVO, RoleVO, RoleDTO, RoleDTO, Role> implements IRoleService {


    private final RoleRepository repository;
    private final SystemQueryProperties queryProperties;
    private final UserRoleRepository userRoleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final AuthorizationCacheService authorizationCacheService;
    private final ObjectProvider<AuthUserCacheService> authUserCacheServiceProvider;
    private final TenantIdProvider tenantIdProvider;

    @Override
    protected BaseRepository<RolePageVO, RoleVO, Role> getRepository() {
        return this.repository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(RoleDTO dto) {
        String id = super.create(dto);
        authorizationCacheService.clearAll();
        return id;
    }

    @Override
    protected void beforeCreate(Role entity, RoleDTO dto) {
        if (entity.getDefaultRegistrationRole() == null) {
            entity.setDefaultRegistrationRole(false);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, RoleDTO dto) {
        super.update(id, dto);
        authorizationCacheService.clearAll();
        refreshAuthUsersByRole(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        Set<String> affectedUserIds = findUserIdsByRole(id);
        super.delete(id);
        authorizationCacheService.clearAll();
        refreshAuthUsers(affectedUserIds);
    }

    @Override
    public List<OptionItem> options() {
        return repository.findByEnabledTrueAndDeletedFalse(PageRequest.of(0, queryProperties.optionLimit())).stream()
                .map(role -> OptionItem.of(role.getRoleName(), role.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public Boolean checkCode(String code) {
        // 返回 true 表示编码可用（不重复），返回 false 表示已存在
        return !repository.existsByRoleCodeAndDeletedFalse(code);
    }

    @Override
    public List<String> getRolePermissions(String id) {
        requireEntity(id);
        return roleMenuRepository.findByRoleId(id).stream()
                .map(RoleMenu::getMenuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignPermissions(String id, List<String> permissionIds) {
        requireEntity(id);
        roleMenuRepository.deleteByRoleId(id);

        List<RoleMenu> roleMenus = permissionIds == null ? List.of() : permissionIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .map(permissionId -> newRoleMenu(id, permissionId))
                .toList();
        roleMenuRepository.saveAll(roleMenus);
        authorizationCacheService.clearAll();
        refreshAuthUsersByRole(id);
        log.info("角色权限分配完成: id={}, permissionCount={}", id, roleMenus.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String duplicate(String id) {
        Role source = requireEntity(id);
        Role copy = new Role();
        copy.setRoleCode(nextCopyRoleCode(source.getRoleCode()));
        copy.setRoleName(source.getRoleName() + "副本");
        copy.setDataScope(source.getDataScope());
        copy.setDefaultRegistrationRole(false);
        copy.setEnabled(source.getEnabled());
        copy.setSortOrder(source.getSortOrder());
        copy.setRemark(source.getRemark());

        Role saved = repository.save(copy);
        List<RoleMenu> roleMenus = roleMenuRepository.findByRoleId(id).stream()
                .map(sourceRoleMenu -> newRoleMenu(saved.getId(), sourceRoleMenu.getMenuId()))
                .toList();
        roleMenuRepository.saveAll(roleMenus);
        authorizationCacheService.clearAll();
        log.info("角色复制完成: sourceId={}, targetId={}, permissionCount={}", id, saved.getId(), roleMenus.size());
        return saved.getId();
    }

    private String nextCopyRoleCode(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            throw new BizException("角色编码不能为空");
        }
        String baseCode = sourceCode + "_copy";
        String candidate = baseCode;
        int sequence = 2;
        while (repository.existsByRoleCodeAndDeletedFalse(candidate)) {
            candidate = baseCode + "_" + sequence++;
        }
        return candidate;
    }

    @Override
    public Map<String, List<String>> getRoleNamesByUserIds(List<String> userIds) {
        List<UserRole> userRoles = userRoleRepository.findByUserIdIn(userIds);
        List<String> allRoleIds = userRoles.stream().map(UserRole::getRoleId).distinct().toList();
        Map<String, String> roleIdToName = repository.findAllById(allRoleIds).stream()
                .collect(Collectors.toMap(Role::getId, Role::getRoleName));
        return userRoles.stream()
                .collect(Collectors.groupingBy(
                        UserRole::getUserId,
                        Collectors.mapping(ur -> roleIdToName.getOrDefault(ur.getRoleId(), ""), Collectors.toList())
                ));
    }

    @Override
    public List<String> getUserRoleIds(String userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getUserRoleCodes(String userId) {
        List<String> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(roleIds).stream()
                .map(Role::getRoleCode)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolesToUser(String userId, List<String> roleIds) {
        userRoleRepository.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRole> userRoles = roleIds.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .map(roleId -> newUserRole(userId, roleId))
                    .collect(Collectors.toList());
            userRoleRepository.saveAll(userRoles);
        }
        authorizationCacheService.evictUser(userId);
        AuthUserCacheService authUserCacheService = authUserCacheServiceProvider.getIfAvailable();
        if (authUserCacheService != null) {
            authUserCacheService.refreshAfterCommit(userId, null, true);
        }
        log.info("用户{}角色分配完成，共{}个角色", userId, roleIds == null ? 0 : roleIds.size());
    }

    private Set<String> findUserIdsByRole(String roleId) {
        return userRoleRepository.findByRoleId(roleId).stream()
                .map(UserRole::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void refreshAuthUsersByRole(String roleId) {
        refreshAuthUsers(findUserIdsByRole(roleId));
    }

    private void refreshAuthUsers(Set<String> userIds) {
        AuthUserCacheService authUserCacheService = authUserCacheServiceProvider.getIfAvailable();
        if (authUserCacheService != null) {
            authUserCacheService.refreshUsersAfterCommit(userIds, true);
        }
    }

    private RoleMenu newRoleMenu(String roleId, String menuId) {
        RoleMenu roleMenu = RoleMenu.builder()
                .roleId(roleId)
                .menuId(menuId)
                .build();
        return RelationEntityDefaults.ensure(roleMenu, tenantIdProvider);
    }

    private UserRole newUserRole(String userId, String roleId) {
        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .build();
        return RelationEntityDefaults.ensure(userRole, tenantIdProvider);
    }
}
