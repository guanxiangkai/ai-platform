package com.ai.system.security;

import com.ai.system.domain.entity.User;
import com.ai.system.repository.UserRepository;
import com.ai.system.service.IDeptService;
import com.ai.system.service.IMenuService;
import com.ai.system.service.IPostService;
import com.ai.system.service.IRoleService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证用户缓存测试。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
class AuthUserCacheServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldWriteAndDeleteTenantScopedUsernameIndex() {
        UserRepository userRepository = mock(UserRepository.class);
        IDeptService deptService = mock(IDeptService.class);
        IMenuService menuService = mock(IMenuService.class);
        IPostService postService = mock(IPostService.class);
        IRoleService roleService = mock(IRoleService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        User user = user();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(postService.getSelectedDeptId(user.getId())).thenReturn(null);
        when(postService.getUserPostCodes(user.getId(), user.getDeptId())).thenReturn(List.of());
        when(roleService.getUserRoleCodes(user.getId())).thenReturn(List.of());
        when(menuService.getUserPermissions(user.getId(), false)).thenReturn(Set.of());
        when(deptService.getUserDeptIds(user.getId(), false)).thenReturn(Set.of(user.getDeptId()));

        AuthUserCacheService service = new AuthUserCacheService(
                userRepository,
                deptService,
                menuService,
                roleService,
                postService,
                redisTemplate,
                mock(JdbcTemplate.class));
        service.refreshAfterCommit(user.getId(), null, false);

        String scopedIndex = "user:auth:username:tenant-a:shared-user";
        verify(valueOperations).set(scopedIndex, user.getId());
        verify(valueOperations, never()).set("user:auth:username:shared-user", user.getId());

        service.evictAfterCommit(user.getId(), user.getTenantId(), user.getUsername());
        verify(redisTemplate).delete("user:auth:" + user.getId());
        verify(redisTemplate).delete(scopedIndex);
    }

    private User user() {
        User user = new User();
        user.setId("user-1");
        user.setTenantId("tenant-a");
        user.setUsername("shared-user");
        user.setPassword("{sha1-bcrypt}$2b$12$" + "A".repeat(53));
        user.setEnabled(true);
        user.setDeptId("dept-1");
        user.setNickname("共享账户");
        user.setUserType("ADMIN");
        return user;
    }
}
