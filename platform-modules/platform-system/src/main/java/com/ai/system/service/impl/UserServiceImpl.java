package com.ai.system.service.impl;

import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.web.plus.core.converter.EntityConverter;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import com.ai.system.constants.SystemConstants;
import com.ai.system.domain.dto.UserCreateDTO;
import com.ai.system.domain.dto.UserDTO;
import com.ai.system.domain.dto.UserPageDTO;
import com.ai.system.domain.entity.User;
import com.ai.system.domain.entity.UserPost;
import com.ai.system.domain.entity.UserRole;
import com.ai.system.domain.vo.UserPageVO;
import com.ai.system.domain.vo.UserVO;
import com.ai.system.repository.*;
import com.ai.system.security.AuthUserCacheService;
import com.ai.system.security.AuthorizationCacheService;
import com.ai.system.service.IPostService;
import com.ai.system.service.IRoleService;
import com.ai.system.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 用户服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends BaseServiceImpl<UserPageDTO, UserPageVO, UserVO, UserCreateDTO, UserDTO, User> implements IUserService {

    private final UserRepository repository;
    private final IRoleService roleService;
    private final IPostService postService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PostRepository postRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserPostRepository userPostRepository;
    private final AuthorizationCacheService authorizationCacheService;
    private final AuthUserCacheService authUserCacheService;
    private final TenantIdProvider tenantIdProvider;

    @Override
    protected BaseRepository<UserPageVO, UserVO, User> getRepository() {
        return this.repository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(UserCreateDTO dto) {
        validateUserUniqueness(dto.username(), dto.email(), dto.phone(), null);

        User user = EntityConverter.toEntity(dto, User.class);
        user.setPassword(passwordEncoder.encode(dto.password()));
        applyAdminFields(user, dto.userType(), false);
        User saved = repository.save(user);

        syncUserRoles(saved.getId(), resolveRoleIdsByCodes(dto.roleCodes()), true);
        syncUserPosts(saved.getId(), resolvePostIdsByCodes(dto.postCodes()), true);
        authorizationCacheService.evictUser(saved.getId());
        authUserCacheService.refreshAfterCommit(saved.getId(), null, false);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, UserDTO dto) {
        User user = repository.findById(id)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        String oldUsername = user.getUsername();

        String username = mergeRequiredText(dto.username(), user.getUsername(), "用户名不能为空");
        String email = mergeNullableText(dto.email(), user.getEmail());
        String phone = mergeNullableText(dto.phone(), user.getPhone());

        validateUserUniqueness(username, email, phone, id);

        user.setUsername(username)
                .setEmail(email)
                .setPhone(phone);
        if (dto.nickname() != null) {
            user.setNickname(normalizeNullableText(dto.nickname()));
        }
        if (dto.realName() != null) {
            user.setRealName(normalizeNullableText(dto.realName()));
        }
        if (dto.gender() != null) {
            user.setGender(dto.gender());
        }
        if (dto.avatar() != null) {
            user.setAvatar(normalizeNullableText(dto.avatar()));
        }
        applyAdminFields(user, dto.userType(), true);

        if (dto.remark() != null) {
            user.setRemark(normalizeNullableText(dto.remark()));
        }
        if (dto.sortOrder() != null) {
            user.setSortOrder(dto.sortOrder());
        }

        if (StringUtils.hasText(dto.password())) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }

        repository.save(user);

        if (dto.roleCodes() != null) {
            syncUserRoles(id, resolveRoleIdsByCodes(dto.roleCodes()), true);
        }
        if (dto.postCodes() != null) {
            syncUserPosts(id, resolvePostIdsByCodes(dto.postCodes()), true);
        }
        authorizationCacheService.evictUser(id);
        authUserCacheService.refreshAfterCommit(id, oldUsername, true);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        User user = repository.findById(id)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        String username = user.getUsername();
        String tenantId = user.getTenantId();
        super.delete(id);
        authorizationCacheService.evictUser(id);
        authUserCacheService.evictAfterCommit(id, tenantId, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnabled(String id, Boolean enabled) {
        super.updateEnabled(id, enabled);
        authorizationCacheService.evictUser(id);
        authUserCacheService.refreshAfterCommit(id, null, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateEnabled(List<String> ids, Boolean enabled) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.stream().filter(Objects::nonNull).distinct().forEach(id -> updateEnabled(id, enabled));
    }

    private void validateUserUniqueness(String username, String email, String phone, String excludeUserId) {
        if (StringUtils.hasText(username)) {
            repository.findByUsernameAndDeletedFalse(username)
                    .filter(u -> !Objects.equals(u.getId(), excludeUserId))
                    .ifPresent(u -> {
                        throw new BizException("用户名已存在");
                    });
        }

        if (StringUtils.hasText(email)) {
            repository.findByEmailAndDeletedFalse(email)
                    .filter(u -> !Objects.equals(u.getId(), excludeUserId))
                    .ifPresent(u -> {
                        throw new BizException("邮箱已存在");
                    });
        }

        if (StringUtils.hasText(phone)) {
            repository.findByPhoneAndDeletedFalse(phone)
                    .filter(u -> !Objects.equals(u.getId(), excludeUserId))
                    .ifPresent(u -> {
                        throw new BizException("手机号已存在");
                    });
        }
    }

    private String mergeRequiredText(String incoming, String current, String message) {
        if (incoming == null) {
            if (StringUtils.hasText(current)) {
                return current;
            }
            throw new BizException(message);
        }
        String normalized = incoming.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(message);
        }
        return normalized;
    }

    private String mergeNullableText(String incoming, String current) {
        if (incoming == null) {
            return current;
        }
        return normalizeNullableText(incoming);
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void syncUserRoles(String userId, List<String> roleIds, boolean replace) {
        if (replace) {
            userRoleRepository.deleteByUserId(userId);
        }
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<UserRole> userRoles = roleIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .map(roleId -> newUserRole(userId, roleId))
                .toList();
        if (!userRoles.isEmpty()) {
            userRoleRepository.saveAll(userRoles);
        }
    }

    private void syncUserPosts(String userId, List<String> postIds, boolean replace) {
        if (replace) {
            userPostRepository.deleteByUserId(userId);
        }
        if (postIds == null || postIds.isEmpty()) {
            return;
        }

        List<String> uniquePostIds = postIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        List<UserPost> userPosts = new java.util.ArrayList<>();
        for (int i = 0; i < uniquePostIds.size(); i++) {
            String postId = uniquePostIds.get(i);
            UserPost userPost = UserPost.builder()
                    .userId(userId)
                    .postId(postId)
                    .mainPost(i == 0)
                    .startDate(java.time.LocalDateTime.now())
                    .build();
            userPosts.add(RelationEntityDefaults.ensure(userPost, tenantIdProvider));
        }
        if (!userPosts.isEmpty()) {
            userPostRepository.saveAll(userPosts);
        }
    }

    private void applyAdminFields(User user, String userType, boolean preserveExisting) {
        String normalizedUserType = StringUtils.hasText(userType)
                ? normalizeUserType(userType)
                : (preserveExisting ? normalizeExistingUserType(user) : SystemConstants.UserConstants.TYPE_USER);
        user.setUserType(normalizedUserType);
    }

    private String normalizeExistingUserType(User user) {
        if (StringUtils.hasText(user.getUserType())) {
            return normalizeUserType(user.getUserType());
        }
        return SystemConstants.UserConstants.TYPE_USER;
    }

    private String normalizeUserType(String userType) {
        if (!StringUtils.hasText(userType)) {
            return SystemConstants.UserConstants.TYPE_USER;
        }
        String normalized = userType.trim().toUpperCase(Locale.ROOT);
        if (!SystemConstants.UserConstants.TYPE_ADMIN.equals(normalized)
                && !SystemConstants.UserConstants.TYPE_USER.equals(normalized)) {
            throw new BizException("用户类型仅支持 ADMIN 或 USER");
        }
        return normalized;
    }

    private List<String> resolveRoleIdsByCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        return roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .map(code -> roleRepository.findByRoleCodeAndDeletedFalse(code)
                        .orElseThrow(() -> new BizException("角色编码不存在: " + code))
                        .getId())
                .toList();
    }

    private List<String> resolvePostIdsByCodes(List<String> postCodes) {
        if (postCodes == null || postCodes.isEmpty()) {
            return List.of();
        }
        return postCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .map(code -> postRepository.findByPostCodeAndDeletedFalse(code)
                        .orElseThrow(() -> new BizException("岗位编码不存在: " + code))
                        .getId())
                .toList();
    }

    private UserRole newUserRole(String userId, String roleId) {
        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .build();
        return RelationEntityDefaults.ensure(userRole, tenantIdProvider);
    }

    @Override
    public PageResponse<UserPageVO> list(UserPageDTO pageDTO) {
        PageResponse<UserPageVO> page = super.list(pageDTO);
        if (page == null || page.isEmpty()) {
            return page;
        }
        List<UserPageVO> records = page.records();
        fillAssociatedNames(records);
        return page;
    }

    /**
     * 批量填充角色名称、岗位名称
     */
    private void fillAssociatedNames(List<UserPageVO> records) {
        List<String> userIds = records.stream().map(UserPageVO::getId).toList();

        Map<String, List<String>> userRoleNames = roleService.getRoleNamesByUserIds(userIds);
        Map<String, List<String>> userPostNames = postService.getPostNamesByUserIds(userIds);

        for (UserPageVO vo : records) {
            vo.setRoleNames(userRoleNames.getOrDefault(vo.getId(), List.of()));
            vo.setPostNames(userPostNames.getOrDefault(vo.getId(), List.of()));
        }
    }

    @Override
    protected Specification<User> buildQuerySpec(UserPageDTO pageDTO) {
        return SpecUtils.<User>builder()
                .andIf(pageDTO != null && StringUtils.hasText(pageDTO.getName()), () -> {
                    String pattern = "%" + pageDTO.getName() + "%";
                    return (root, query, cb) -> cb.or(
                            cb.like(root.get("username"), pattern),
                            cb.like(root.get("nickname"), pattern),
                            cb.like(root.get("realName"), pattern)
                    );
                })
                .eqIfPresent(User::getEnabled, pageDTO != null ? pageDTO.getEnabled() : null)
                .eqIfPresent(User::getUserType, pageDTO != null ? normalizeUserTypeForQuery(pageDTO.getUserType()) : null)
                .build();
    }

    private String normalizeUserTypeForQuery(String userType) {
        if (!StringUtils.hasText(userType)) {
            return null;
        }
        return normalizeUserType(userType);
    }

    @Override
    protected Sort buildSort(UserPageDTO pageDTO) {
        Sort.Order primary = null;
        if (StringUtils.hasText(pageDTO.getSortBy())) {
            primary = pageDTO.isAsc()
                    ? Sort.Order.asc(pageDTO.getSortBy())
                    : Sort.Order.desc(pageDTO.getSortBy());
        }
        Sort.Order fallback = Sort.Order.desc("createTime");
        if (primary != null) {
            return Sort.by(primary, fallback);
        }
        return Sort.by(fallback);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean changePassword(String id, String oldPassword, String newPassword) {
        User user = repository.findById(id)
                .orElseThrow(() -> BizException.notFound("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("旧密码错误: id={}", id);
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
        authUserCacheService.refreshAfterCommit(id, null, true);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(String id) {
        User user = repository.findById(id)
                .orElseThrow(() -> BizException.notFound("用户不存在"));

        String newPassword = UUID.randomUUID().toString().replace("-", "");
        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
        authUserCacheService.refreshAfterCommit(id, null, true);
        return newPassword;
    }

    @Override
    public Boolean checkUsername(String username) {
        return !repository.existsByUsernameAndDeletedFalse(username);
    }

    @Override
    public List<String> getUserRoles(String id) {
        return roleService.getUserRoleIds(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignRoles(String id, List<String> roleIds, String userType) {
        User user = repository.findById(id)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        if (StringUtils.hasText(userType)) {
            applyAdminFields(user, userType, true);
            repository.save(user);
        }
        roleService.assignRolesToUser(id, roleIds);
        authorizationCacheService.evictUser(id);
        return true;
    }

    @Override
    public UserVO findByUsername(String username) {
        return repository.findByUsernameAndDeletedFalse(username)
                .map(user -> EntityConverter.toVo(user, UserVO.class))
                .orElse(null);
    }

    @Override
    public Boolean existsByEmail(String email) {
        return repository.existsByEmailAndDeletedFalse(email);
    }
}
