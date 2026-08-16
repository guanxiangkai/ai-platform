package com.ai.api.system.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 导入模板字段映射 DTO（API 契约层）
 * <p>
 * 由 platform-system-api 统一定义，供所有消费方共享。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public record ImportTemplateFieldDTO(
        String id,
        String templateModule,
        String templateId,
        List<String> title,
        String field,
        Boolean exactMatch,
        Boolean required,
        Boolean multiple,
        Boolean enabled,
        Boolean repeat,
        String remark
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
