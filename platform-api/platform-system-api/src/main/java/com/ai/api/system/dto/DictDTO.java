package com.ai.api.system.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典 DTO（API 契约层）
 * <p>
 * 由 ai-system-api 统一定义，供所有消费方共享。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public record DictDTO(
        String id,
        String type,
        String label,
        String value,
        String color,
        String cssClass,
        Boolean isDefault,
        Boolean enabled,
        Integer sort,
        String remark
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
