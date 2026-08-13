package com.ai.api.system.dto;

import java.io.Serializable;

/**
 * 平台用户在当前租户内的可对外身份摘要。
 *
 * @param userId      用户标识
 * @param username    登录账号
 * @param displayName 展示名称
 * @param deptId      主部门标识
 * @param deptName    主部门名称
 * @param unitName    统计单位名称；存在上级部门时取上级，否则取主部门
 */
public record UserIdentityDTO(
        String userId,
        String username,
        String displayName,
        String deptId,
        String deptName,
        String unitName
) implements Serializable {
}
