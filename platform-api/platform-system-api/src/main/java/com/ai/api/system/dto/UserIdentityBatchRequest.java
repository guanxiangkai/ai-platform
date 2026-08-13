package com.ai.api.system.dto;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 按用户名批量查询平台用户身份的请求。
 *
 * @param usernames 当前租户内待查询的用户名，最多 500 个
 */
public record UserIdentityBatchRequest(List<String> usernames) implements Serializable {

    private static final int MAX_USERNAMES = 500;

    /** 规范化用户名、去重并限制单次查询规模。 */
    public UserIdentityBatchRequest {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (usernames != null) {
            usernames.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .forEach(normalized::add);
        }
        if (normalized.size() > MAX_USERNAMES) {
            throw new IllegalArgumentException("单次最多查询 " + MAX_USERNAMES + " 个用户名");
        }
        usernames = List.copyOf(normalized);
    }
}
