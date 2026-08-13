package com.ai.system.service.impl;

import com.ai.api.system.dto.PushPreferenceBatchRequest;
import com.ai.api.system.dto.UserPushPreferenceDTO;
import com.ai.system.domain.dto.SettingDTO;
import com.ai.system.domain.entity.Setting;
import com.ai.system.domain.entity.User;
import com.ai.system.domain.vo.SettingVO;
import com.ai.system.repository.SettingRepository;
import com.ai.system.repository.UserRepository;
import com.ai.system.service.ISettingService;
import io.github.guanxiangkai.web.plus.core.converter.EntityConverter;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 当前用户的平台设置服务实现。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements ISettingService {

    private final SettingRepository repository;
    private final UserRepository userRepository;

    @Override
    public SettingVO getCurrentUserSetting() {
        String userId = requireCurrentUserId();
        return repository.findByOwnerIdAndDeletedFalse(userId)
                .map(setting -> EntityConverter.toVo(setting, SettingVO.class))
                .orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettingVO saveCurrentUserSetting(SettingDTO settingDTO) {
        String userId = requireCurrentUserId();
        Setting setting = repository.findByOwnerIdAndDeletedFalse(userId)
                .orElseGet(() -> newSetting(userId));

        applyPatch(setting, settingDTO);
        return EntityConverter.toVo(repository.save(setting), SettingVO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPushPreferenceDTO> getPushPreferences(PushPreferenceBatchRequest request) {
        List<String> userIds = request == null ? List.of() : request.userIds();
        if (userIds.isEmpty()) {
            return List.of();
        }

        Set<String> tenantUserIds = userRepository.findAllByIdInAndDeletedFalse(userIds).stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        if (tenantUserIds.isEmpty()) {
            return List.of();
        }

        List<String> authorizedUserIds = userIds.stream()
                .filter(tenantUserIds::contains)
                .toList();
        Map<String, Setting> settingsByOwnerId = repository
                .findAllByOwnerIdInAndDeletedFalse(authorizedUserIds)
                .stream()
                .collect(Collectors.toMap(
                        Setting::getOwnerId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return authorizedUserIds.stream()
                .map(userId -> {
                    Setting setting = settingsByOwnerId.get(userId);
                    return new UserPushPreferenceDTO(
                            userId,
                            setting == null || !Boolean.FALSE.equals(setting.getDesktopNotification())
                    );
                })
                .toList();
    }

    private Setting newSetting(String userId) {
        Setting setting = new Setting();
        setting.setOwnerId(userId);
        setting.setExtensions(new LinkedHashMap<>());
        return setting;
    }

    private void applyPatch(Setting setting, SettingDTO dto) {
        if (dto.language() != null) setting.setLanguage(dto.language());
        if (dto.theme() != null) setting.setTheme(dto.theme());
        if (dto.fontSize() != null) setting.setFontSize(dto.fontSize());
        if (dto.desktopNotification() != null) setting.setDesktopNotification(dto.desktopNotification());
        if (dto.soundNotification() != null) setting.setSoundNotification(dto.soundNotification());
        if (dto.emailNotification() != null) setting.setEmailNotification(dto.emailNotification());
        if (dto.notificationFrequency() != null) setting.setNotificationFrequency(dto.notificationFrequency());
        if (dto.autoSave() != null) setting.setAutoSave(dto.autoSave());
        if (dto.loginProtection() != null) setting.setLoginProtection(dto.loginProtection());
        if (dto.sessionTimeout() != null) setting.setSessionTimeout(dto.sessionTimeout());
        if (dto.extensions() != null) {
            setting.setExtensions(mergeExtensions(setting.getExtensions(), dto.extensions()));
        }
    }

    private Map<String, Object> mergeExtensions(Map<String, Object> current, Map<String, Object> patch) {
        Map<String, Object> merged = new LinkedHashMap<>(current == null ? Map.of() : current);
        patch.forEach((namespace, value) -> {
            if (!StringUtils.hasText(namespace)) throw new BizException("设置扩展命名空间不能为空");
            String normalizedNamespace = namespace.trim();
            if (value == null) merged.remove(normalizedNamespace);
            else merged.put(normalizedNamespace, value);
        });
        return merged;
    }

    private String requireCurrentUserId() {
        String userId = SecurityUtils.getUserId();
        if (!StringUtils.hasText(userId)) throw new BizException("当前登录用户无效");
        return userId;
    }
}
