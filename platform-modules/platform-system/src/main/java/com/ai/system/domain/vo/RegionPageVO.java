package com.ai.system.domain.vo;

import com.ai.system.domain.entity.Region;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 行政区域分页VO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "行政区域分页VO")
@AutoMapper(target = Region.class)
public class RegionPageVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private String id;

    @Schema(description = "区域编码")
    private String regionCode;

    @Schema(description = "区域名称")
    private String regionName;

    @Schema(description = "父区域ID")
    private String parentId;

    @Schema(description = "层级（province=省, city=市, district=区/县, street=街道）")
    private String regionLevel;

    @Schema(description = "完整路径名称")
    private String fullName;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
