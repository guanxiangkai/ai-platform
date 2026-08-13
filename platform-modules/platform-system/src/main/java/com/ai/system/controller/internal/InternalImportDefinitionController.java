package com.ai.system.controller.internal;

import com.ai.api.system.dto.ImportDefinitionDTO;
import com.ai.system.service.IImportTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Internal configuration contract consumed by the base configurable import framework. */
@RestController
@RequestMapping("/internal/import-definition")
@RequiredArgsConstructor
public class InternalImportDefinitionController {
    private final IImportTemplateService importTemplateService;

    @GetMapping("/enabled")
    public List<ImportDefinitionDTO> enabled(@RequestParam("tenantId") String tenantId) {
        return importTemplateService.getEnabledDefinitions(tenantId);
    }
}
