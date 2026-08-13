package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 行政区域分页查询DTO
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "行政区域分页查询DTO")
public class RegionPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "区域编码")
    private String regionCode;

    @Schema(description = "区域名称（模糊查询）")
    private String regionName;

    @Schema(description = "父区域ID")
    private String parentId;

    @Schema(description = "层级（province=省, city=市, district=区/县, street=街道）")
    private String regionLevel;
}
