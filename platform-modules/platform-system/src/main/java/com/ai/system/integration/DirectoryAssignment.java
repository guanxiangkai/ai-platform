package com.ai.system.integration;

/**
 * 租户目录主体的当前分配信息。
 *
 * @param assignmentId 分配项 ID
 * @param assignmentName 分配项名称
 * @param groupId 分组 ID
 * @param groupName 分组名称
 */
public record DirectoryAssignment(
        String assignmentId,
        String assignmentName,
        String groupId,
        String groupName
) {
}
