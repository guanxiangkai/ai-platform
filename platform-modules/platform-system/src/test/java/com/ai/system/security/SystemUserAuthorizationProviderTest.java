package com.ai.system.security;

import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.security.authorization.AuthorizationScope;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SystemUserAuthorizationProviderTest {

    private static final String USER_ID = "user-1";

    @Test
    void loadShouldReadAuthorizationScopeFromSharedAuthCache() {
        AuthorizationCacheService authorizationCacheService = mock(AuthorizationCacheService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        SystemUserAuthorizationProvider provider = new SystemUserAuthorizationProvider(
                authorizationCacheService,
                redisTemplate
        );

        when(authorizationCacheService.get(eq(USER_ID), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<AuthorizationScope> loader = invocation.getArgument(1, Supplier.class);
            return loader.get();
        });
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(AuthConstants.UserAuthCacheConstants.USER_AUTH_CACHE_PREFIX + USER_ID))
                .thenReturn(Map.of(
                        AuthConstants.UserAuthCacheConstants.FIELD_ROLE_CODES, "[\"OPERATOR\"]",
                        AuthConstants.UserAuthCacheConstants.FIELD_PERMISSIONS,
                        "[\"system:menu:list\",\"system:dept:list\"]",
                        AuthConstants.UserAuthCacheConstants.FIELD_DEPT_IDS, "[\"dept-1\"]"
                ));

        AuthorizationScope scope = provider.load(USER_ID, false);

        assertThat(scope.roles()).containsExactly("OPERATOR");
        assertThat(scope.permissions()).containsExactlyInAnyOrder("system:menu:list", "system:dept:list");
        assertThat(scope.deptIds()).containsExactly("dept-1");
    }

    @Test
    void loadShouldNotAccessTenantDataBeforeContextExistsForSuperAdmin() {
        AuthorizationCacheService authorizationCacheService = mock(AuthorizationCacheService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SystemUserAuthorizationProvider provider = new SystemUserAuthorizationProvider(
                authorizationCacheService,
                redisTemplate
        );

        AuthorizationScope scope = provider.load(USER_ID, true);

        assertThat(scope).isSameAs(AuthorizationScope.EMPTY);
        verifyNoInteractions(authorizationCacheService, redisTemplate);
    }
}
