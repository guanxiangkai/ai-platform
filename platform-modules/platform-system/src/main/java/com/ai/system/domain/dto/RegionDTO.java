package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Region;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 行政区域创建/更新DTO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "行政区域DTO")
@AutoMapper(target = Region.class)
public record RegionDTO(
        @NotBlank(message = "区域编码不能为空") @Schema(description = "区域编码（GB/T 2260）") String regionCode,
        @NotBlank(message = "区域名称不能为空") @Schema(description = "区域名称") String regionName,
        @NotBlank(message = "父区域ID不能为空") @Schema(description = "父区域ID（根节点为'0'）") String parentId,
        @NotBlank(message = "层级不能为空") @Schema(description = "层级（province=省, city=市, district=区/县, street=街道）") String regionLevel,
        @Schema(description = "区域简称") String shortName,
        @Schema(description = "经度") BigDecimal longitude,
        @Schema(description = "纬度") BigDecimal latitude,
        @Schema(description = "邮政编码") String zipCode,
        @Schema(description = "备注") String remark
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
