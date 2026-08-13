package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.query.wrapper.QueryWrapper;
import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.ImportTemplate;
import com.ai.system.domain.vo.ImportTemplatePageVO;
import com.ai.system.domain.vo.ImportTemplateVO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 导入模板数据访问层
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface ImportTemplateRepository
        extends BaseRepository<ImportTemplatePageVO, ImportTemplateVO, ImportTemplate>,
                JpaPlusRepository<ImportTemplate, String> {

    /** 根据模块和模板编码查询（过滤逻辑删除） */
    default Optional<ImportTemplate> findByTemplateModuleAndTemplateCodeAndDeletedFalse(
            String templateModule, String templateCode) {
        return one(QueryWrapper.from(ImportTemplate.class)
                .eq(ImportTemplate::getTemplateModule, templateModule)
                .eq(ImportTemplate::getTemplateCode, templateCode)
                .eq(ImportTemplate::getDeleted, false));
    }

    /** 根据模块查询已启用的模板（过滤逻辑删除） */
    default Optional<ImportTemplate> findByTemplateModuleAndEnabledTrueAndDeletedFalse(String templateModule) {
        return one(QueryWrapper.from(ImportTemplate.class)
                .eq(ImportTemplate::getTemplateModule, templateModule)
                .eq(ImportTemplate::getEnabled, true)
                .eq(ImportTemplate::getDeleted, false));
    }

    default List<ImportTemplate> findEnabledByTenantId(String tenantId) {
        return list(QueryWrapper.from(ImportTemplate.class)
                .eq(ImportTemplate::getTenantId, tenantId)
                .eq(ImportTemplate::getEnabled, true)
                .eq(ImportTemplate::getDeleted, false)
                .orderByAsc(ImportTemplate::getSortOrder)
                .orderByAsc(ImportTemplate::getTemplateName));
    }
}
