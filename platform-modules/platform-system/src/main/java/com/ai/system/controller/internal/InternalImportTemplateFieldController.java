package com.ai.system.controller.internal;

import com.ai.system.domain.vo.ImportTemplateFieldVO;
import com.ai.system.service.IImportTemplateFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 导入模板字段映射内部接口（仅供微服务间 RPC 调用）
 * <p>
 * 路径前缀 {@code /internal/import-mapping}，由网关白名单控制访问，不对外暴露。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RestController
@RequestMapping("/internal/import-mapping")
@RequiredArgsConstructor
public class InternalImportTemplateFieldController {

    private final IImportTemplateFieldService importTemplateFieldService;

    /**
     * 根据模块获取导入字段映射列表
     */
    @GetMapping("/by-module")
    public List<ImportTemplateFieldVO> getByModule(@RequestParam("module") String module) {
        return importTemplateFieldService.getByModule(module);
    }

    /**
     * 根据模块和字段获取导入字段映射
     */
    @GetMapping("/by-module-and-field")
    public ImportTemplateFieldVO getByModuleAndField(@RequestParam("module") String module,
                                                     @RequestParam("field") String field) {
        return importTemplateFieldService.getByModuleAndField(module, field);
    }
}
