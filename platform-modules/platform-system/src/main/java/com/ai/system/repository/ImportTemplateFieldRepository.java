package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.query.wrapper.QueryWrapper;
import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.ImportTemplateField;
import com.ai.system.domain.vo.ImportTemplateFieldPageVO;
import com.ai.system.domain.vo.ImportTemplateFieldVO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 导入模板字段映射数据访问层
 * <p>
 * 联表查询逻辑（按模块查字段）已移至 Service 层两步完成：
 * 先查 {@link ImportTemplateRepository} 获取模板，再查本 Repository 获取字段。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface ImportTemplateFieldRepository
        extends BaseRepository<ImportTemplateFieldPageVO, ImportTemplateFieldVO, ImportTemplateField>,
                JpaPlusRepository<ImportTemplateField, String> {

    /** 按模板ID查询已启用且未删除的字段列表，按排序号升序 */
    default List<ImportTemplateField> findByTemplateId(String templateId) {
        return list(QueryWrapper.from(ImportTemplateField.class)
                .eq(ImportTemplateField::getTemplateId, templateId)
                .eq(ImportTemplateField::getEnabled, true)
                .eq(ImportTemplateField::getDeleted, false)
                .orderByAsc(ImportTemplateField::getSortOrder)
                .orderByAsc(ImportTemplateField::getCreateTime));
    }

    /** 按模板ID和字段名查询单条未删除记录 */
    default Optional<ImportTemplateField> findByTemplateIdAndField(String templateId, String field) {
        return one(QueryWrapper.from(ImportTemplateField.class)
                .eq(ImportTemplateField::getTemplateId, templateId)
                .eq(ImportTemplateField::getField, field)
                .eq(ImportTemplateField::getDeleted, false));
    }
}
