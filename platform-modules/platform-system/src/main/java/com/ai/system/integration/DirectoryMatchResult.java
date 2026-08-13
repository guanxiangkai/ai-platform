package com.ai.system.integration;

/**
 * 租户目录返回的注册主体匹配结果。
 *
 * @param subjectId 目录主体 ID；未匹配时为空
 * @param displayName 目录主体显示名称
 * @param alreadyLinked 是否已经绑定平台账户
 */
public record DirectoryMatchResult(
        String subjectId,
        String displayName,
        boolean alreadyLinked
) {
}
