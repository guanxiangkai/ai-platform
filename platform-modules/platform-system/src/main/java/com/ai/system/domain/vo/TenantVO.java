package com.ai.system.domain.vo;

import com.ai.system.domain.entity.Tenant;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "租户VO")
@AutoMapper(target = Tenant.class)
public class TenantVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "联系邮箱")
    private String contactEmail;

    @Schema(description = "租户地址")
    private String address;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "用户数量限制")
    private Integer userLimit;

    @Schema(description = "租户Logo")
    private String logo;

    @Schema(description = "租户域名")
    private String domain;

    @Schema(description = "租户描述")
    private String description;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;
}
