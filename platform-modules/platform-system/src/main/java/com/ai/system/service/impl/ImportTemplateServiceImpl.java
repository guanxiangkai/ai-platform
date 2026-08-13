package com.ai.system.service.impl;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import com.ai.api.system.dto.ImportDefinitionDTO;
import com.ai.api.system.dto.ImportDefinitionFieldDTO;
import com.ai.system.domain.dto.ImportTemplateCreateDTO;
import com.ai.system.domain.dto.ImportTemplateDTO;
import com.ai.system.domain.dto.ImportTemplatePageDTO;
import com.ai.system.domain.entity.ImportTemplate;
import com.ai.system.domain.vo.ImportTemplatePageVO;
import com.ai.system.domain.vo.ImportTemplateVO;
import com.ai.system.repository.ImportTemplateRepository;
import com.ai.system.repository.ImportTemplateFieldRepository;
import com.ai.system.service.IImportTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 导入模板服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ImportTemplateServiceImpl extends BaseServiceImpl<ImportTemplatePageDTO, ImportTemplatePageVO, ImportTemplateVO, ImportTemplateCreateDTO, ImportTemplateDTO, ImportTemplate> implements IImportTemplateService {

    private final ImportTemplateRepository repository;
    private final ImportTemplateFieldRepository fieldRepository;

    @Override
    protected BaseRepository<ImportTemplatePageVO, ImportTemplateVO, ImportTemplate> getRepository() {
        return this.repository;
    }

    @Override
    protected void beforeCreate(ImportTemplate entity, ImportTemplateCreateDTO dto) {
        if (!StringUtils.hasText(entity.getId())) {
            entity.setId(UUID.randomUUID().toString());
        }
        normalize(entity);
    }

    @Override
    protected void beforeUpdate(ImportTemplate entity, ImportTemplateDTO dto) {
        normalize(entity);
    }

    @Override
    protected Specification<ImportTemplate> buildQuerySpec(ImportTemplatePageDTO pageDTO) {
        if (pageDTO == null) return (root, query, cb) -> cb.conjunction();
        return SpecUtils.<ImportTemplate>builder()
                .likeIfPresent(ImportTemplate::getTemplateModule, pageDTO.getTemplateModule())
                .likeIfPresent(ImportTemplate::getTemplateCode, pageDTO.getTemplateCode())
                .likeIfPresent(ImportTemplate::getTemplateName, pageDTO.getTemplateName())
                .eqIfPresent(ImportTemplate::getFileType, pageDTO.getFileType())
                .eqIfPresent(ImportTemplate::getEnabled, pageDTO.getEnabled())
                .build();
    }

    @Override
    protected Sort buildSort(ImportTemplatePageDTO pageDTO) {
        return Sort.by(
                Sort.Order.asc("sortInfo.sortOrder").nullsLast(),
                Sort.Order.asc("templateName")
        );
    }

    @Override
    public List<ImportDefinitionDTO> getEnabledDefinitions(String tenantId) {
        if (!StringUtils.hasText(tenantId)) return List.of();
        return repository.findEnabledByTenantId(tenantId.trim()).stream()
                .map(template -> new ImportDefinitionDTO(
                        template.getTemplateCode(),
                        template.getTemplateName(),
                        effectiveFileNamePatterns(template),
                        List.of(template.getFileType()),
                        effectiveSheetNames(template),
                        template.getTargetSchema(),
                        template.getTargetTable(),
                        Boolean.TRUE.equals(template.getCustomImportEnabled()),
                        template.getHandlerKey(),
                        template.getWriteMode(),
                        template.getHeaderRowIndex() == null ? 0 : template.getHeaderRowIndex(),
                        template.getBatchSize(),
                        fieldRepository.findByTemplateId(template.getId()).stream()
                                .map(field -> new ImportDefinitionFieldDTO(
                                        field.getField(),
                                        field.getTitle(),
                                        field.getTargetColumn(),
                                        field.getDataType(),
                                        field.getFormatPattern(),
                                        field.getDefaultValue(),
                                        field.getConverterKey(),
                                        Boolean.TRUE.equals(field.getExactMatch()),
                                        Boolean.TRUE.equals(field.getRequired()),
                                        Boolean.TRUE.equals(field.getMultiple()),
                                        Boolean.TRUE.equals(field.getRepeat()),
                                        field.getSortOrder() == null ? 0 : field.getSortOrder()
                                ))
                                .toList()
                ))
                .toList();
    }

    private void normalize(ImportTemplate entity) {
        entity.setFileNamePatterns(normalizeTextList(entity.getFileNamePatterns()));
        entity.setSheetNames(normalizeTextList(entity.getSheetNames()));
        if (entity.getCustomImportEnabled() == null) entity.setCustomImportEnabled(true);
        if (!StringUtils.hasText(entity.getHandlerKey()) && Boolean.TRUE.equals(entity.getCustomImportEnabled())) {
            entity.setHandlerKey(entity.getTemplateModule());
        }
        entity.setWriteMode(StringUtils.hasText(entity.getWriteMode())
                ? entity.getWriteMode().trim().toUpperCase(Locale.ROOT)
                : "INSERT");
        entity.setHeaderRowIndex(entity.getHeaderRowIndex() == null || entity.getHeaderRowIndex() < 0
                ? 0 : entity.getHeaderRowIndex());
        entity.setBatchSize(entity.getBatchSize() == null || entity.getBatchSize() <= 0 ? 500 : entity.getBatchSize());
        if (!StringUtils.hasText(entity.getTargetSchema())) entity.setTargetSchema("public");
    }

    private List<String> effectiveFileNamePatterns(ImportTemplate template) {
        List<String> configured = normalizeTextList(template.getFileNamePatterns());
        if (!configured.isEmpty()) return configured;
        String extension = StringUtils.hasText(template.getFileType()) ? template.getFileType().trim() : "xlsx";
        return List.of("*" + template.getTemplateCode() + "*." + extension,
                "*" + template.getTemplateName() + "*." + extension);
    }

    private List<String> effectiveSheetNames(ImportTemplate template) {
        return normalizeTextList(template.getSheetNames());
    }

    private List<String> normalizeTextList(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

}
