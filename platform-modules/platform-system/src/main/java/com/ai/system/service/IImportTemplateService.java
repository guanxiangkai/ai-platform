package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.system.domain.dto.ImportTemplateCreateDTO;
import com.ai.system.domain.dto.ImportTemplateDTO;
import com.ai.system.domain.dto.ImportTemplatePageDTO;
import com.ai.system.domain.entity.ImportTemplate;
import com.ai.system.domain.vo.ImportTemplatePageVO;
import com.ai.system.domain.vo.ImportTemplateVO;
import com.ai.api.system.dto.ImportDefinitionDTO;

import java.util.List;

/**
 * 导入模板服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IImportTemplateService extends IBaseService<ImportTemplatePageDTO, ImportTemplatePageVO, ImportTemplateVO, ImportTemplateCreateDTO, ImportTemplateDTO, ImportTemplate> {
    List<ImportDefinitionDTO> getEnabledDefinitions(String tenantId);
}
