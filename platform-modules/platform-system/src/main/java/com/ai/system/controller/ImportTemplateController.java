package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.system.domain.dto.ImportTemplateCreateDTO;
import com.ai.system.domain.dto.ImportTemplateDTO;
import com.ai.system.domain.dto.ImportTemplatePageDTO;
import com.ai.system.domain.entity.ImportTemplate;
import com.ai.system.domain.vo.ImportTemplatePageVO;
import com.ai.system.domain.vo.ImportTemplateVO;
import com.ai.system.service.IImportTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 导入模板管理控制器
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "导入模板管理", description = "导入模板的增删改查功能")
@RestController
@RequestMapping("/system/import-template")
@RequiredArgsConstructor
public class ImportTemplateController extends BaseController<ImportTemplatePageDTO, ImportTemplatePageVO, ImportTemplateVO, ImportTemplateCreateDTO, ImportTemplateDTO, ImportTemplate> {

    private final IImportTemplateService service;

    @Override
    protected IBaseService<ImportTemplatePageDTO, ImportTemplatePageVO, ImportTemplateVO, ImportTemplateCreateDTO, ImportTemplateDTO, ImportTemplate> getService() {
        return this.service;
    }

}
