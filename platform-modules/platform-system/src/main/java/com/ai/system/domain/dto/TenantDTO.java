package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Tenant;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户数据传输对象。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "租户DTO")
@AutoMapper(target = Tenant.class)
public record TenantDTO(
        @Schema(description = "主键ID（更新时必填）") String id,
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "租户名称") String tenantName,
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
