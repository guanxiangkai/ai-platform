package com.ai.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户岗位VO（岗位选择/切换场景使用）
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "用户岗位VO")
public record UserPostVO(
        @Schema(description = "岗位ID") String postId,
        @Schema(description = "岗位名称") String positionName,
        @Schema(description = "岗位编码") String positionCode,
        @Schema(description = "岗位所属部门ID") String deptId,
        @Schema(description = "岗位所属部门名称") String deptName,
        @Schema(description = "岗位类型：借调岗、临时岗等") String postType,
        @Schema(description = "是否主岗") Boolean isPrimary,
        @Schema(description = "任职开始日期") LocalDateTime startDate,
        @Schema(description = "任职结束日期") LocalDateTime endDate,
        @Schema(description = "是否当前选中") Boolean selected
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
