package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Tenant;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户DTO创建DTO
 * <p>
 * 用于创建接口入参，包含参数校验；更新接口使用 {@link TenantDTO}
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "租户DTO创建DTO")
@AutoMapper(target = Tenant.class)
public record TenantCreateDTO(
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @NotBlank(message = "租户名称不能为空") @Schema(description = "租户名称") String tenantName,
        @Schema(description = "租户编码") String tenantCode,
        @Schema(description = "联系人") String contactName,
        @Schema(description = "联系电话") String contactPhone,
        @Schema(description = "联系邮箱") String contactEmail,
        @Schema(description = "租户地址") String address,
        @Schema(description = "过期时间") LocalDateTime expireTime,
        @Schema(description = "用户数量限制") Integer userLimit,
        @Schema(description = "租户Logo") String logo,
        @Schema(description = "租户域名") String domain,
        @Schema(description = "租户描述") String description,
        @Schema(description = "排序") Integer sort
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
