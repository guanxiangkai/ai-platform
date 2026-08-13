package com.ai.system.security;

import com.ai.system.config.AuthorizationCacheProperties;
import io.github.guanxiangkai.redis.plus.cache.ThreeLevelCacheTemplate;
import io.github.guanxiangkai.web.plus.security.authorization.AuthorizationScope;
import com.ai.system.constants.SystemConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 用户授权范围三级缓存。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthorizationCacheProperties.class)
public class AuthorizationCacheService {
    private final ObjectProvider<ThreeLevelCacheTemplate> cacheTemplateProvider;
    private final AuthorizationCacheProperties properties;

    public AuthorizationScope get(String userId, Supplier<AuthorizationScope> loader) {
        ThreeLevelCacheTemplate cacheTemplate = requireCacheTemplate();
        AuthorizationScope scope = cacheTemplate.get(
                SystemConstants.CacheConstants.AUTHORIZATION_SCOPE_CACHE_NAME,
                userId,
                AuthorizationScope.class,
                properties.getTtl(),
                key -> loader.get());
        return scope != null ? scope : AuthorizationScope.EMPTY;
    }

    public void evictUser(String userId) {
        try {
            requireCacheTemplate().evict(SystemConstants.CacheConstants.AUTHORIZATION_SCOPE_CACHE_NAME, userId);
        } catch (Exception e) {
            log.warn("清理用户授权缓存失败: userId={}", userId, e);
        }
    }

    public void clearAll() {
        try {
            requireCacheTemplate().clear(SystemConstants.CacheConstants.AUTHORIZATION_SCOPE_CACHE_NAME);
        } catch (Exception e) {
            log.warn("清理全部用户授权缓存失败", e);
        }
    }

    private ThreeLevelCacheTemplate requireCacheTemplate() {
        ThreeLevelCacheTemplate cacheTemplate = cacheTemplateProvider.getIfAvailable();
        if (cacheTemplate == null) {
            throw new IllegalStateException("三级缓存模板未初始化");
        }
        return cacheTemplate;
    }
}
