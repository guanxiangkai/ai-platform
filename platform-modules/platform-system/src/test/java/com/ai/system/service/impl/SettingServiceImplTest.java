package com.ai.system.service.impl;

import com.ai.api.system.dto.PushPreferenceBatchRequest;
import com.ai.system.domain.entity.Setting;
import com.ai.system.domain.entity.User;
import com.ai.system.repository.SettingRepository;
import com.ai.system.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SettingServiceImplTest {

    @Test
    void pushPreferencesShouldRestrictToTenantUsersAndDefaultMissingSettingsToEnabled() {
        SettingRepository repository = mock(SettingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        SettingServiceImpl service = new SettingServiceImpl(repository, userRepository);
        Setting disabled = setting("user-2", false);
        Setting enabled = setting("user-1", true);
        PushPreferenceBatchRequest request = new PushPreferenceBatchRequest(
                List.of("user-1", "user-2", "user-3", "other-tenant-user"));
        when(userRepository.findAllByIdInAndDeletedFalse(request.userIds()))
                .thenReturn(List.of(user("user-1"), user("user-2"), user("user-3")));
        when(repository.findAllByOwnerIdInAndDeletedFalse(List.of("user-1", "user-2", "user-3")))
                .thenReturn(List.of(disabled, enabled));

        var result = service.getPushPreferences(request);

        assertThat(result)
                .extracting(preference -> preference.userId() + ":" + preference.pushEnabled())
                .containsExactly("user-1:true", "user-2:false", "user-3:true");
        verify(userRepository).findAllByIdInAndDeletedFalse(request.userIds());
        verify(repository).findAllByOwnerIdInAndDeletedFalse(List.of("user-1", "user-2", "user-3"));
    }

    @Test
    void emptyBatchShouldNotQueryRepository() {
        SettingRepository repository = mock(SettingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        SettingServiceImpl service = new SettingServiceImpl(repository, userRepository);

        assertThat(service.getPushPreferences(new PushPreferenceBatchRequest(List.of()))).isEmpty();

        verifyNoInteractions(repository, userRepository);
    }

    private Setting setting(String ownerId, Boolean desktopNotification) {
        Setting setting = new Setting();
        setting.setOwnerId(ownerId);
        setting.setDesktopNotification(desktopNotification);
        return setting;
    }

    private User user(String id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
