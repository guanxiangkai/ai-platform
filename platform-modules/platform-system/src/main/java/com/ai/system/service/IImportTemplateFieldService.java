package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.system.domain.dto.ImportTemplateFieldCreateDTO;
import com.ai.system.domain.dto.ImportTemplateFieldDTO;
import com.ai.system.domain.dto.ImportTemplateFieldPageDTO;
import com.ai.system.domain.entity.ImportTemplateField;
import com.ai.system.domain.vo.ImportTemplateFieldPageVO;
import com.ai.system.domain.vo.ImportTemplateFieldVO;

import java.util.List;

/**
 * 导入模板字段映射服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IImportTemplateFieldService extends IBaseService<ImportTemplateFieldPageDTO, ImportTemplateFieldPageVO, ImportTemplateFieldVO, ImportTemplateFieldCreateDTO, ImportTemplateFieldDTO, ImportTemplateField> {

    /**
     * 根据模块获取导入字段映射列表
     *
     * @param module 模块
     * @return 导入字段映射列表
     */
    List<ImportTemplateFieldVO> getByModule(String module);

    /**
     * 根据模块和字段获取导入字段映射
     *
     * @param module 模块
     * @param field  字段
     * @return 导入字段映射
     */
    ImportTemplateFieldVO getByModuleAndField(String module, String field);
}
