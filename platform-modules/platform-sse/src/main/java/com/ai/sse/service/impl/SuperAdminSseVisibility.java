package com.ai.sse.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import io.github.guanxiangkai.web.plus.core.entity.BaseEntity;
import io.github.guanxiangkai.web.plus.core.exception.CoreBizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

/**
 * SSE 管理查询的超级管理员记录可见性规则。
 *
 * <p>超级管理员身份必须同时通过安全上下文标志和平台固定用户 ID 校验；推送目标包含平台超级管理员
 * 时，普通身份无法通过只查询单个目标用户来绕过整条记录的隐藏规则。多目标字段按生产端约定的逗号
 * 分隔字符串保存，并按完整 token 匹配。</p>
 */
final class SuperAdminSseVisibility {

    private SuperAdminSseVisibility() {
    }

    static boolean canViewSuperAdminRecords() {
        return SecurityUtils.isSuperAdmin()
                && PlatformSuperAdmin.USER_ID.equals(SecurityUtils.getUserId());
    }

    static <T extends BaseEntity> Specification<T> userIdSpecification() {
        return (root, query, cb) -> {
            if (canViewSuperAdminRecords()) {
                return cb.isFalse(root.get("deleted"));
            }
            return cb.and(
                    cb.isFalse(root.get("deleted")),
                    cb.or(cb.isNull(root.get("userId")),
                            cb.notEqual(root.get("userId"), PlatformSuperAdmin.USER_ID)),
                    cb.or(cb.isNull(root.get("createBy")),
                            cb.notEqual(root.get("createBy"), PlatformSuperAdmin.USER_ID)),
                    cb.or(cb.isNull(root.get("updateBy")),
                            cb.notEqual(root.get("updateBy"), PlatformSuperAdmin.USER_ID))
            );
        };
    }

    static <T extends BaseEntity> Specification<T> pushSpecification() {
        return (root, query, cb) -> {
            if (canViewSuperAdminRecords()) {
                return cb.isFalse(root.get("deleted"));
            }
            return cb.and(
                    cb.isFalse(root.get("deleted")),
                    cb.or(cb.isNull(root.get("userId")),
                            cb.notEqual(root.get("userId"), PlatformSuperAdmin.USER_ID)),
                    cb.or(cb.isNull(root.get("userIds")),
                            cb.and(
                                    cb.notEqual(root.get("userIds"), PlatformSuperAdmin.USER_ID),
                                    cb.notLike(root.get("userIds"), PlatformSuperAdmin.USER_ID + ",%"),
                                    cb.notLike(root.get("userIds"), "%," + PlatformSuperAdmin.USER_ID),
                                    cb.notLike(root.get("userIds"), "%," + PlatformSuperAdmin.USER_ID + ",%"))),
                    cb.or(cb.isNull(root.get("createBy")),
                            cb.notEqual(root.get("createBy"), PlatformSuperAdmin.USER_ID)),
                    cb.or(cb.isNull(root.get("updateBy")),
                            cb.notEqual(root.get("updateBy"), PlatformSuperAdmin.USER_ID))
            );
        };
    }

    static boolean isVisible(String userId, String createBy, String updateBy) {
        if (canViewSuperAdminRecords()) {
            return true;
        }
        return !isSuperAdminSubject(userId)
                && !isSuperAdminSubject(createBy)
                && !isSuperAdminSubject(updateBy);
    }

    static boolean isVisible(String userId, String userIds, String createBy, String updateBy) {
        if (canViewSuperAdminRecords()) {
            return true;
        }
        return !isSuperAdminSubject(userId)
                && !containsSuperAdmin(userIds)
                && !isSuperAdminSubject(createBy)
                && !isSuperAdminSubject(updateBy);
    }

    static void requireVisible(String userId, String createBy, String updateBy,
                               String entityName, String id) {
        if (!isVisible(userId, createBy, updateBy)) {
            throw CoreBizException.notFound(entityName, id);
        }
    }

    static void requireVisible(String userId, String userIds, String createBy, String updateBy,
                               String entityName, String id) {
        if (!isVisible(userId, userIds, createBy, updateBy)) {
            throw CoreBizException.notFound(entityName, id);
        }
    }

    private static boolean isSuperAdminSubject(String userId) {
        return PlatformSuperAdmin.USER_ID.equals(userId);
    }

    private static boolean containsSuperAdmin(String userIds) {
        if (userIds == null) return false;
        return userIds.equals(PlatformSuperAdmin.USER_ID)
                || userIds.startsWith(PlatformSuperAdmin.USER_ID + ",")
                || userIds.endsWith("," + PlatformSuperAdmin.USER_ID)
                || userIds.contains("," + PlatformSuperAdmin.USER_ID + ",");
    }
}
