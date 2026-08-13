package com.ai.system.security;

import cn.hutool.json.JSONUtil;
import com.ai.api.security.PlatformAuthCacheKeys;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import com.ai.api.context.TenantExecutionScope;
import com.ai.system.domain.entity.User;
import com.ai.system.repository.UserRepository;
import com.ai.system.service.IDeptService;
import com.ai.system.service.IMenuService;
import com.ai.system.service.IPostService;
import com.ai.system.service.IRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * system 侧维护 auth 所需的用户认证概要缓存。
 */
@Slf4j
@Service
public class AuthUserCacheService {

    private final UserRepository userRepository;
    private final IDeptService deptService;
    private final IMenuService menuService;
    private final IRoleService roleService;
    private final IPostService postService;
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    public AuthUserCacheService(
            UserRepository userRepository,
            IDeptService deptService,
            IMenuService menuService,
            IRoleService roleService,
            IPostService postService,
            @Qualifier("authStringRedisTemplate") StringRedisTemplate redisTemplate,
            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.deptService = deptService;
        this.menuService = menuService;
        this.roleService = roleService;
        this.postService = postService;
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void refreshAfterCommit(String userId, String oldUsername, boolean incrementTokenVersion) {
        runAfterCommit(() -> refresh(userId, oldUsername, incrementTokenVersion));
    }

    public void evictAfterCommit(String userId, String tenantId, String username) {
        runAfterCommit(() -> evict(userId, tenantId, username));
    }

    public void refreshUsersAfterCommit(Collection<String> userIds, boolean incrementTokenVersion) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        runAfterCommit(() -> userIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .forEach(userId -> refresh(userId, null, incrementTokenVersion)));
    }

    public void refreshAll() {
        try {
            java.util.List<TenantUserRef> users = jdbcTemplate.query("""
                            SELECT id, tenant_id
                            FROM sys_user
                            WHERE coalesce(deleted, false) = false
                              AND tenant_id IS NOT NULL
                              AND btrim(tenant_id) <> ''
                            """,
                    (resultSet, rowNum) -> new TenantUserRef(
                            resultSet.getString("id"), resultSet.getString("tenant_id")));
            users.forEach(user -> TenantExecutionScope.run(
                    user.tenantId(), () -> refresh(user.userId(), null, false)));
            log.info("[AuthUserCache] 用户认证缓存按租户预热完成: count={}", users.size());
        } catch (Exception e) {
            log.error("[AuthUserCache] 用户认证缓存预热失败", e);
        }
    }

    private void refresh(String userId, String oldUsername, boolean incrementTokenVersion) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                evict(userId, null, oldUsername);
                return;
            }

            String key = authKey(userId);
            Map<String, String> values = toHash(user);
            redisTemplate.opsForHash().putAll(key, values);
            redisTemplate.opsForHash().delete(
                    key,
                    AuthConstants.UserAuthCacheConstants.FIELD_SUPER_ADMIN
            );
            redisTemplate.opsForHash().putIfAbsent(
                    key,
                    AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION,
                    "1"
            );
            if (incrementTokenVersion) {
                redisTemplate.opsForHash().increment(
                        key,
                        AuthConstants.UserAuthCacheConstants.FIELD_TOKEN_VERSION,
                        1
                );
            }

            redisTemplate.opsForValue().set(
                    PlatformAuthCacheKeys.usernameIndex(user.getTenantId(), user.getUsername()),
                    userId);
            if (StringUtils.hasText(oldUsername) && !oldUsername.equals(user.getUsername())) {
                redisTemplate.delete(PlatformAuthCacheKeys.usernameIndex(user.getTenantId(), oldUsername));
            }
        } catch (Exception e) {
            log.error("[AuthUserCache] 同步用户认证缓存失败: userId={}, oldUsername={}, incrementTokenVersion={}",
                    userId, oldUsername, incrementTokenVersion, e);
        }
    }

    private void evict(String userId, String tenantId, String username) {
        try {
            if (StringUtils.hasText(userId)) {
                redisTemplate.delete(authKey(userId));
            }
            if (StringUtils.hasText(tenantId) && StringUtils.hasText(username)) {
                redisTemplate.delete(PlatformAuthCacheKeys.usernameIndex(tenantId, username));
            }
        } catch (Exception e) {
            log.error("[AuthUserCache] 删除用户认证缓存失败: userId={}, username={}", userId, username, e);
        }
    }

    private Map<String, String> toHash(User user) {
        String deptId = user.getDeptId();
        String selectedDeptId = postService.getSelectedDeptId(user.getId());
        if (StringUtils.hasText(selectedDeptId)) {
            deptId = selectedDeptId;
        }
        java.util.Collection<String> postCodes = StringUtils.hasText(deptId)
                ? postService.getUserPostCodes(user.getId(), deptId)
                : postService.getUserPostCodes(user.getId());

        Map<String, String> hash = new LinkedHashMap<>();
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_ID, user.getId());
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_USERNAME, user.getUsername());
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_PASSWORD_HASH, user.getPassword());
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_ENABLED, String.valueOf(Boolean.TRUE.equals(user.getEnabled())));
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_NICKNAME, user.getNickname());
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_AVATAR, user.getAvatar());
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_USER_TYPE, user.getUserType());
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_TENANT_ID, user.getTenantId());
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_DEPT_ID, deptId);
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_ROLE_CODES, collectionJson(roleService.getUserRoleCodes(user.getId())));
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_POST_CODES, collectionJson(postCodes));
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_PERMISSIONS, collectionJson(menuService.getUserPermissions(user.getId(), false)));
        // 业务数据范围始终按当前部门下所有有效岗位的数据权限并集计算。
        put(hash, AuthConstants.UserAuthCacheConstants.FIELD_DEPT_IDS, collectionJson(deptService.getUserDeptIds(user.getId(), false)));
        return hash;
    }

    private String collectionJson(java.util.Collection<String> values) {
        return JSONUtil.toJsonStr(values == null ? java.util.List.of() : values);
    }

    private void put(Map<String, String> hash, String field, String value) {
        hash.put(field, value == null ? "" : value);
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private String authKey(String userId) {
        return AuthConstants.UserAuthCacheConstants.USER_AUTH_CACHE_PREFIX + userId;
    }

    private record TenantUserRef(String userId, String tenantId) {
    }
}
