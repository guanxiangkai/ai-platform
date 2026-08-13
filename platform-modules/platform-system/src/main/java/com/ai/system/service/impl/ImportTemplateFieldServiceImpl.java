package com.ai.system.service.impl;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import com.ai.system.domain.dto.ImportTemplateFieldCreateDTO;
import com.ai.system.domain.dto.ImportTemplateFieldDTO;
import com.ai.system.domain.dto.ImportTemplateFieldPageDTO;
import com.ai.system.domain.entity.ImportTemplate;
import com.ai.system.domain.entity.ImportTemplateField;
import com.ai.system.domain.vo.ImportTemplateFieldPageVO;
import com.ai.system.domain.vo.ImportTemplateFieldVO;
import com.ai.system.repository.ImportTemplateFieldRepository;
import com.ai.system.repository.ImportTemplateRepository;
import com.ai.system.service.IImportTemplateFieldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 导入模板字段映射服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportTemplateFieldServiceImpl extends BaseServiceImpl<ImportTemplateFieldPageDTO, ImportTemplateFieldPageVO, ImportTemplateFieldVO, ImportTemplateFieldCreateDTO, ImportTemplateFieldDTO, ImportTemplateField> implements IImportTemplateFieldService {


    private final ImportTemplateFieldRepository repository;
    private final ImportTemplateRepository importTemplateRepository;

    @Override
    protected BaseRepository<ImportTemplateFieldPageVO, ImportTemplateFieldVO, ImportTemplateField> getRepository() {
        return this.repository;
    }

    @Override
    protected Specification<ImportTemplateField> buildQuerySpec(ImportTemplateFieldPageDTO pageDTO) {
        if (pageDTO == null) return (root, query, cb) -> cb.conjunction();
        Specification<ImportTemplateField> specification = SpecUtils.<ImportTemplateField>builder()
                .eqIfPresent(ImportTemplateField::getTemplateId, pageDTO.getTemplateId())
                .likeIfPresent(ImportTemplateField::getField, pageDTO.getField())
                .eqIfPresent(ImportTemplateField::getExactMatch, pageDTO.getExactMatch())
                .eqIfPresent(ImportTemplateField::getEnabled, pageDTO.getEnabled())
                .eqIfPresent(ImportTemplateField::getRequired, pageDTO.getRequired())
                .eqIfPresent(ImportTemplateField::getRepeat, pageDTO.getRepeat())
                .eqIfPresent(ImportTemplateField::getMultiple, pageDTO.getMultiple())
                .build();
        if (!StringUtils.hasText(pageDTO.getTitle())) {
            return specification;
        }
        String title = pageDTO.getTitle().trim().toLowerCase();
        return specification.and((root, query, cb) -> cb.like(
                cb.lower(cb.function(
                        "array_to_string",
                        String.class,
                        root.get("title"),
                        cb.literal(",")
                )),
                "%" + title + "%"
        ));
    }

    @Override
    protected Sort buildSort(ImportTemplateFieldPageDTO pageDTO) {
        return Sort.by(
                Sort.Order.asc("sortInfo.sortOrder").nullsLast(),
                Sort.Order.asc("field")
        );
    }

    @Override
    protected void beforeCreate(ImportTemplateField entity, ImportTemplateFieldCreateDTO dto) {
        if (!StringUtils.hasText(entity.getId())) {
            entity.setId(UUID.randomUUID().toString());
        }
        entity.setTitle(normalizeTitles(dto.title()));
        normalize(entity);
    }

    @Override
    protected void beforeUpdate(ImportTemplateField entity, ImportTemplateFieldDTO dto) {
        entity.setTitle(normalizeTitles(dto.title()));
        normalize(entity);
    }

    @Override
    public List<ImportTemplateFieldVO> getByModule(String module) {
        // 先查模板，再按 templateId 查字段（避免 JPQL JOIN 字符串）
        Optional<ImportTemplate> template = importTemplateRepository
                .findByTemplateModuleAndEnabledTrueAndDeletedFalse(module);
        if (template.isEmpty()) {
            return List.of();
        }
        List<ImportTemplateField> configs = repository.findByTemplateId(template.get().getId());
        return configs.stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    @Override
    public ImportTemplateFieldVO getByModuleAndField(String module, String field) {
        Optional<ImportTemplate> template = importTemplateRepository
                .findByTemplateModuleAndEnabledTrueAndDeletedFalse(module);
        if (template.isEmpty()) {
            return null;
        }
        Optional<ImportTemplateField> config = repository
                .findByTemplateIdAndField(template.get().getId(), field);
        return config.map(this::toVo).orElse(null);
    }

    private ImportTemplateFieldVO toVo(ImportTemplateField config) {
        ImportTemplateFieldVO vo = new ImportTemplateFieldVO();
        vo.setId(config.getId());
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        vo.setRemark(config.getRemark());
        vo.setEnabled(config.getEnabled());
        vo.setSortOrder(config.getSortOrder());
        vo.setTemplateId(config.getTemplateId());
        vo.setTitle(config.getTitle());
        vo.setField(config.getField());
        vo.setTargetColumn(config.getTargetColumn());
        vo.setDataType(config.getDataType());
        vo.setFormatPattern(config.getFormatPattern());
        vo.setDefaultValue(config.getDefaultValue());
        vo.setConverterKey(config.getConverterKey());
        vo.setExactMatch(config.getExactMatch());
        vo.setRequired(config.getRequired());
        vo.setMultiple(config.getMultiple());
        vo.setRepeat(config.getRepeat());
        return vo;
    }

    private List<String> normalizeTitles(List<String> titles) {
        if (titles == null) {
            return List.of();
        }
        return titles.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private void normalize(ImportTemplateField entity) {
        if (!StringUtils.hasText(entity.getDataType())) entity.setDataType("STRING");
        if (!StringUtils.hasText(entity.getTargetColumn())) entity.setTargetColumn(entity.getField());
    }
}
