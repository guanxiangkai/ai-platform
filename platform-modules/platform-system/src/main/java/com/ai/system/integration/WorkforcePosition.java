package com.ai.system.integration;

/**
 * 租户人员当前岗位信息。
 *
 * @param positionId 岗位 ID
 * @param positionName 岗位名称
 * @param deptId 部门 ID
 * @param deptName 部门名称
 */
public record WorkforcePosition(
        String positionId,
        String positionName,
        String deptId,
        String deptName
) {
}
