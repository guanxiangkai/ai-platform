package com.ai.api.system.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 租户内用户推送偏好批量查询请求。
 *
 * <p>批量大小是内部 HTTP 契约的安全边界，请求中的空值、空白值和重复用户标识
 * 会在序列化边界统一归一化。</p>
 *
 * @param userIds 用户标识列表
 * @author guanxiangkai
 * @since 1.0.0
 */
public record PushPreferenceBatchRequest(List<String> userIds) implements Serializable {

    /** 单次批量查询允许的最大用户数。 */
    public static final int MAX_USER_IDS = 500;

    public PushPreferenceBatchRequest {
        userIds = userIds == null ? List.of() : userIds.stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (userIds.size() > MAX_USER_IDS) {
            throw new IllegalArgumentException("单次推送偏好查询不能超过 " + MAX_USER_IDS + " 个用户");
        }
    }
}
