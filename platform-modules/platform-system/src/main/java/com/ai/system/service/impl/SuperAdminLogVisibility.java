package com.ai.system.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import io.github.guanxiangkai.web.plus.core.exception.CoreBizException;
import io.github.guanxiangkai.web.plus.log.entity.BaseLog;
import org.springframework.data.jpa.domain.Specification;

/**
 * 限制所有管理端查询访问超级管理员审计日志。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
final class SuperAdminLogVisibility {

    private SuperAdminLogVisibility() {
    }

    static <T extends BaseLog> Specification<T> specification() {
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
        if (PlatformSuperAdmin.USER_ID.equals(log.getUserId())) {
            throw CoreBizException.notFound(entityName, id);
        }
    }
}
