package com.ai.system.security;

import com.ai.api.security.PlatformSuperAdmin;
import io.github.guanxiangkai.web.plus.error.exception.PermissionDeniedException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 在 Controller 和 Service 目标边界同时执行固定平台超级管理员校验。 */
@Aspect
@Component
@Order(-100)
public class PlatformSuperAdminGuardAspect {

    /** 按运行时资源类型校验，涵盖基础 Controller/Service 的继承方法。 */
    @Around("execution(public * *(..)) && (target(com.ai.system.controller.TenantController) || target(com.ai.system.service.impl.TenantServiceImpl))")
    public Object requirePlatformSuperAdmin(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!SecurityUtils.isSuperAdmin() || !PlatformSuperAdmin.USER_ID.equals(SecurityUtils.getUserId())) {
            throw new PermissionDeniedException("仅平台超级管理员可以管理租户");
        }
        return joinPoint.proceed();
    }
}
