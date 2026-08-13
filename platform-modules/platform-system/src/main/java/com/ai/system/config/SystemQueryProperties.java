package com.ai.system.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 系统管理查询窗口配置。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Validated
@ConfigurationProperties(prefix = "platform.system.query")
public record SystemQueryProperties(
        @Min(1) @Max(500) Integer optionLimit,
        @Min(1) @Max(5_000) Integer maintenanceBatchSize
) {

    public SystemQueryProperties {
        optionLimit = optionLimit == null ? 100 : optionLimit;
        maintenanceBatchSize = maintenanceBatchSize == null ? 500 : maintenanceBatchSize;
        if (optionLimit <= 0 || optionLimit > 500) {
            throw new IllegalArgumentException("系统选项数量必须在 1 到 500 之间");
        }
        if (maintenanceBatchSize <= 0 || maintenanceBatchSize > 5_000) {
            throw new IllegalArgumentException("系统维护批量必须在 1 到 5000 之间");
        }
    }

    /**
     * 创建安全默认配置。
     *
     * @return 默认查询窗口
     */
    public static SystemQueryProperties defaults() {
        return new SystemQueryProperties(null, null);
    }
}
