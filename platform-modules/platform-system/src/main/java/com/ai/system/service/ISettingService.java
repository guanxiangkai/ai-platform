package com.ai.system.service;

import com.ai.api.system.dto.PushPreferenceBatchRequest;
import com.ai.api.system.dto.UserPushPreferenceDTO;
import com.ai.system.domain.dto.SettingDTO;
import com.ai.system.domain.vo.SettingVO;

import java.util.List;

/**
 * 当前用户的平台设置服务。
 *
 * <p>设置只能通过当前认证用户访问，接口不接受所有者或记录 ID，避免产生跨用户读写入口。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface ISettingService {

    /**
     * 查询当前登录用户的设置。
     *
     * @return 尚未保存时返回 {@code null}
     */
    SettingVO getCurrentUserSetting();

    /**
     * 按非空字段合并保存当前登录用户的设置。
     *
     * @param settingDTO 设置补丁
     * @return 保存后的完整设置
     */
    SettingVO saveCurrentUserSetting(SettingDTO settingDTO);

    /**
     * 在当前租户边界内批量查询用户推送偏好。
     *
     * @param request 批量查询请求
     * @return 当前租户内用户的推送偏好；用户未配置时默认允许推送
     */
    List<UserPushPreferenceDTO> getPushPreferences(PushPreferenceBatchRequest request);
}
