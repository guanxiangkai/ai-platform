package com.ai.api.system.dto;

/**
 * 平台组织部门的最小跨服务契约。
 *
 * @param deptId 部门标识
 * @param deptName 部门名称
 * @param parentDeptId 父部门标识；顶级部门为空
 * @param organizationUnitName 实际组织单位名称；存在父部门时取父部门名称，否则取本部门名称
 */
public record DeptIdentityDTO(
        String deptId,
        String deptName,
        String parentDeptId,
        String organizationUnitName
) {
}
