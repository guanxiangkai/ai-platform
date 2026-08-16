package com.ai.system.security;

import cn.hutool.json.JSONUtil;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.security.authorization.AuthorizationScope;
import io.github.guanxiangkai.web.plus.security.authorization.UserAuthorizationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * platform-system 本地授权范围加载器。
 * <p>
 * 下游认证过滤器建立租户上下文之前会调用本组件，因此这里只能读取由 system 侧维护的共享认证概要，
 * 不能查询受租户隔离的业务表。
 * </p>
 */
@Slf4j
@Component
public class SystemUserAuthorizationProvider implements UserAuthorizationProvider {

    private final AuthorizationCacheService authorizationCacheService;
    private final StringRedisTemplate redisTemplate;

    public SystemUserAuthorizationProvider(
            AuthorizationCacheService authorizationCacheService,
            @Qualifier("authStringRedisTemplate") StringRedisTemplate redisTemplate) {
        this.authorizationCacheService = authorizationCacheService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 从共享认证概要加载用户授权范围。
     *
     * @param userId     当前用户 ID
     * @param superAdmin 是否为超级管理员
     * @return 角色、功能权限和组织数据范围；超级管理员、内部服务或缓存缺失时返回空范围
     */
    @Override
    public AuthorizationScope load(String userId, boolean superAdmin) {
        if (superAdmin) {
            return AuthorizationScope.EMPTY;
        }
        if (!StringUtils.hasText(userId)
                || AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID.equals(userId)) {
            return AuthorizationScope.EMPTY;
        }
        return authorizationCacheService.get(userId, () -> loadFromAuthCache(userId));
    }

    private AuthorizationScope loadFromAuthCache(String userId) {
        try {
            Map<Object, Object> hash = redisTemplate.opsForHash().entries(authKey(userId));
            if (hash == null || hash.isEmpty()) {
                log.warn("系统服务未找到用户认证概要，按空授权范围处理");
                return AuthorizationScope.EMPTY;
            }
            return new AuthorizationScope(
                    stringSet(hash, AuthConstants.UserAuthCacheConstants.FIELD_ROLE_CODES),
                    stringSet(hash, AuthConstants.UserAuthCacheConstants.FIELD_PERMISSIONS),
                    stringSet(hash, AuthConstants.UserAuthCacheConstants.FIELD_DEPT_IDS)
            );
        } catch (Exception e) {
            log.warn("系统服务加载用户授权范围失败，按空授权范围处理: exception={}",
                    e.getClass().getSimpleName());
            return AuthorizationScope.EMPTY;
        }
    }

    private Set<String> stringSet(Map<Object, Object> hash, String field) {
        String json = value(hash, field);
        if (!StringUtils.hasText(json)) {
            return Set.of();
        }
        try {
            return JSONUtil.parseArray(json)
                    .toList(String.class)
                    .stream()
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (Exception e) {
            log.warn("解析用户授权范围字段失败: field={}, exception={}",
                    field, e.getClass().getSimpleName());
            return Set.of();
        }
    }

    private String value(Map<Object, Object> hash, String field) {
        Object value = hash.get(field);
        return value == null ? null : value.toString();
    }

    private String authKey(String userId) {
        return AuthConstants.UserAuthCacheConstants.USER_AUTH_CACHE_PREFIX + userId;
    }
}
