package com.ai.api.system.dto;

/**
 * 平台用户的组织归属契约。
 *
 * @param userId 用户标识
 * @param deptId 所属部门标识；未分配部门时为空
 */
public record UserOrganizationDTO(String userId, String deptId) {
}
