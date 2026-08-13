package com.ai.system.service.impl;

import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.web.plus.core.entity.TenantEntity;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 关系表实体通过 Spring Data 直接保存时，补齐 jpa-plus 字段引擎原本负责的基础字段。
 */
public final class RelationEntityDefaults {

    private RelationEntityDefaults() {
    }

    public static <T extends TenantEntity> T ensure(T entity, TenantIdProvider tenantIdProvider) {
        if (!StringUtils.hasText(entity.getId())) {
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (!StringUtils.hasText(entity.getTenantId())) {
            entity.setTenantId(tenantIdProvider.getCurrentTenantId());
        }

        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(now);
        }
        if (entity.getUpdateTime() == null) {
            entity.setUpdateTime(now);
        }

        String currentUserId = SecurityUtils.getUserId();
        if (!StringUtils.hasText(entity.getCreateBy())) {
            entity.setCreateBy(currentUserId);
        }
        if (!StringUtils.hasText(entity.getUpdateBy())) {
            entity.setUpdateBy(currentUserId);
        }
        if (entity.getDeleted() == null) {
            entity.setDeleted(false);
        }
        if (entity.getVersion() == null) {
            entity.setVersion(0);
        }
        return entity;
    }
}
