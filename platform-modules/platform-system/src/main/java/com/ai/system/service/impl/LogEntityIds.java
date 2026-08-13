package com.ai.system.service.impl;

import io.github.guanxiangkai.web.plus.core.entity.BaseEntity;
import io.github.guanxiangkai.web.plus.log.entity.BaseLog;
import com.ai.system.constants.SystemConstants.LogConstants;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

final class LogEntityIds {

    private LogEntityIds() {
    }

    static void ensureRequiredFields(BaseLog entity) {
        ensureId(entity);
        if (!StringUtils.hasText(entity.getTenantId())) {
            entity.setTenantId(LogConstants.DEFAULT_TENANT_ID);
        }
        if (!StringUtils.hasText(entity.getLocation())) {
            entity.setLocation("");
        }

        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(now);
        }
        if (entity.getUpdateTime() == null) {
            entity.setUpdateTime(now);
        }
        if (!StringUtils.hasText(entity.getCreateBy())) {
            entity.setCreateBy(entity.getUserId());
        }
        if (!StringUtils.hasText(entity.getUpdateBy())) {
            entity.setUpdateBy(entity.getUserId());
        }
    }

    private static void ensureId(BaseEntity entity) {
        if (!StringUtils.hasText(entity.getId())) {
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
        }
    }
}
