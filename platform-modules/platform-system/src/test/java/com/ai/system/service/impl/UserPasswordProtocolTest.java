package com.ai.system.service.impl;

import com.ai.api.security.PasswordDigestProtocol;
import com.ai.api.security.ProtocolPasswordEncoder;
import com.ai.system.repository.PostRepository;
import com.ai.system.repository.RoleRepository;
import com.ai.system.repository.UserPostRepository;
import com.ai.system.repository.UserRepository;
import com.ai.system.repository.UserRoleRepository;
import com.ai.system.security.AuthUserCacheService;
import com.ai.system.security.AuthorizationCacheService;
import com.ai.system.domain.entity.User;
import com.ai.system.service.IPostService;
import com.ai.system.service.IRoleService;
import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPasswordProtocolTest {

    @Test
    void changePasswordAcceptsDigestAndRejectsRawValue() {
        ProtocolPasswordEncoder encoder = new ProtocolPasswordEncoder();
        String oldDigest = PasswordDigestProtocol.sha1Utf8("old-password");
        String newDigest = PasswordDigestProtocol.sha1Utf8("new-password");
        User user = new User();
        user.setId("user-1");
        user.setPassword(encoder.encode(oldDigest));
        UserRepository repository = mock(UserRepository.class);
        when(repository.findById("user-1")).thenReturn(Optional.of(user));
        UserServiceImpl service = service(repository, encoder);

        assertThat(service.changePassword("user-1", oldDigest, newDigest)).isTrue();
        assertThat(encoder.matches(newDigest, user.getPassword())).isTrue();
        assertThat(service.changePassword("user-1", "old-password", newDigest)).isFalse();
    }

    @Test
    void resetPasswordStoresSubmittedDigestWithoutReturningPassword() {
        ProtocolPasswordEncoder encoder = new ProtocolPasswordEncoder();
        String resetDigest = PasswordDigestProtocol.sha1Utf8("admin-local-random-password");
        User user = new User();
        user.setId("user-1");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findById("user-1")).thenReturn(Optional.of(user));
        UserServiceImpl service = service(repository, encoder);

        Boolean reset = service.resetPassword("user-1", resetDigest);

        assertThat(reset).isTrue();
        assertThat(encoder.matches(resetDigest, user.getPassword())).isTrue();
        assertThat(encoder.matches("admin-local-random-password", user.getPassword())).isFalse();
        assertThatIllegalArgumentException().isThrownBy(() -> service.resetPassword("user-1", "raw-password"));
    }

    private static UserServiceImpl service(UserRepository repository, ProtocolPasswordEncoder encoder) {
        return new UserServiceImpl(
                repository,
                mock(IRoleService.class),
                mock(IPostService.class),
                encoder,
                mock(RoleRepository.class),
                mock(PostRepository.class),
                mock(UserRoleRepository.class),
                mock(UserPostRepository.class),
                mock(AuthorizationCacheService.class),
                mock(AuthUserCacheService.class),
                mock(TenantIdProvider.class)
        );
    }
}
