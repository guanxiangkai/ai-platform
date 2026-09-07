package com.ai.system.integration;

import org.springframework.util.StringUtils;

/**
 * 租户目录返回的注册主体匹配结果。
 *
 * @param subjectId 当前租户目录的稳定不透明主体 ID；未匹配时为空
 * @param displayName 目录主体显示名称
 * @param alreadyLinked 是否已经绑定平台账户
 */
public record DirectoryMatchResult(
        String subjectId,
        String displayName,
        boolean alreadyLinked
) {

    /**
     * 外部目录匹配结果进入平台前的 ID 契约。
     *
     * <p>主体 ID 是外部目录的稳定不透明值，平台不得改写它；非空值必须可存入注册记录，
     * 且不得带有会改变目录身份的首尾空白。</p>
     */
    public DirectoryMatchResult {
        if (subjectId != null) {
            requireIdentityId(subjectId, "目录主体 ID");
        }
    }

    /** 校验可在目录与平台之间传递的稳定身份 ID，且不改写其不透明值。 */
    static void requireIdentityId(String value, String fieldName) {
        if (!StringUtils.hasText(value) || value.length() > 64 || !value.equals(value.strip())) {
            throw new IllegalArgumentException(fieldName + "必须为不含首尾空白且长度不超过64字符的非空 ID");
        }
    }
}
