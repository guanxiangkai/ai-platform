package com.ai.system.integration;

/**
 * 租户目录主体的当前分配信息。
 *
 * @param assignmentId 外部目录分配项 ID，不是平台主键
 * @param assignmentName 分配项名称
 * @param groupId 外部目录分组 ID，不是平台注册部门主键
 * @param groupName 分组名称
 */
public record DirectoryAssignment(
        String assignmentId,
        String assignmentName,
        String groupId,
        String groupName
) {
}
