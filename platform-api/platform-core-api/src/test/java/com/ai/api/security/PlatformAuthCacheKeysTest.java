package com.ai.api.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 平台认证缓存键测试。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
class PlatformAuthCacheKeysTest {

    @Test
    void shouldScopeUsernameIndexByTenant() {
        assertThat(PlatformAuthCacheKeys.usernameIndex(" tenant-a ", " shared-user "))
                .isEqualTo("user:auth:username:tenant-a:shared-user");
    }

    @Test
    void shouldRejectBlankTenantOrUsername() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PlatformAuthCacheKeys.usernameIndex(" ", "user"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PlatformAuthCacheKeys.usernameIndex("tenant", null));
    }
}
