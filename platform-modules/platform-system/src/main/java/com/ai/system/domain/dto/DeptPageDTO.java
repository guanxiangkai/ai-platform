package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 部门分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "部门分页查询参数")
public class DeptPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 部门名称（模糊查询）
     */
    @Schema(description = "部门名称（模糊查询）")
    private String deptName;

    /**
     * 部门编码（模糊查询）
     */
    @Schema(description = "部门编码（模糊查询）")
    private String deptCode;

    /**
     * 位置（模糊查询）
     */
    @Schema(description = "位置（模糊查询）")
    private String location;

    /**
     * 区域编码
     */
    @Schema(description = "区域编码")
    private String regionCode;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    private Boolean enabled;
}
