package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户分页查询DTO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户分页查询DTO")
public class TenantPageDTO extends PageRequest {

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
