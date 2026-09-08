package com.ai.system.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import io.github.guanxiangkai.web.plus.core.exception.CoreBizException;
import io.github.guanxiangkai.web.plus.log.entity.BaseLog;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

/**
 * 限制管理端查询访问超级管理员审计日志。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
final class SuperAdminLogVisibility {

    private SuperAdminLogVisibility() {
    }

    static <T extends BaseLog> Specification<T> specification() {
        if (canViewSuperAdminLogs()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("userId")),
                cb.notEqual(root.get("userId"), PlatformSuperAdmin.USER_ID)
        );
    }

    static void requireVisible(
            BaseLog log,
            String entityName,
            String id
    ) {
        if (!canViewSuperAdminLogs() && PlatformSuperAdmin.USER_ID.equals(log.getUserId())) {
            throw CoreBizException.notFound(entityName, id);
        }
    }

    /**
     * 只有安全上下文确认的超级管理员，且其真实用户 ID 为平台超级管理员 ID 时，才能查看该类日志。
     * 请求参数、请求头或普通用户自带的标志不能改变此判断。
     */
    static boolean canViewSuperAdminLogs() {
        return SecurityUtils.isSuperAdmin()
                && PlatformSuperAdmin.USER_ID.equals(SecurityUtils.getUserId());
    }
}
