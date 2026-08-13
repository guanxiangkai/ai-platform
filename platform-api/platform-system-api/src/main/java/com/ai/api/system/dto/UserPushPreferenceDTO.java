package com.ai.api.system.dto;

import java.io.Serializable;

/**
 * 租户内用户推送偏好。
 *
 * @param userId      用户标识
 * @param pushEnabled 是否允许桌面推送
 * @author guanxiangkai
 * @since 1.0.0
 */
public record UserPushPreferenceDTO(String userId, boolean pushEnabled) implements Serializable {
}
