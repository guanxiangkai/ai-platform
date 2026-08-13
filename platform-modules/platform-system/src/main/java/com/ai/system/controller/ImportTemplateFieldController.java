package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.system.domain.dto.ImportTemplateFieldCreateDTO;
import com.ai.system.domain.dto.ImportTemplateFieldDTO;
import com.ai.system.domain.dto.ImportTemplateFieldPageDTO;
import com.ai.system.domain.entity.ImportTemplateField;
import com.ai.system.domain.vo.ImportTemplateFieldPageVO;
import com.ai.system.domain.vo.ImportTemplateFieldVO;
import com.ai.system.service.IImportTemplateFieldService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 导入模板字段映射管理控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "导入模板字段映射管理", description = "导入模板字段映射的增删改查功能")
@RestController
@RequestMapping("/system/import-mapping")
@RequiredArgsConstructor
public class ImportTemplateFieldController extends BaseController<ImportTemplateFieldPageDTO, ImportTemplateFieldPageVO, ImportTemplateFieldVO, ImportTemplateFieldCreateDTO, ImportTemplateFieldDTO, ImportTemplateField> {

    private final IImportTemplateFieldService service;

    @Override
    protected IBaseService<ImportTemplateFieldPageDTO, ImportTemplateFieldPageVO, ImportTemplateFieldVO, ImportTemplateFieldCreateDTO, ImportTemplateFieldDTO, ImportTemplateField> getService() {
        return this.service;
    }


}
