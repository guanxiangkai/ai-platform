package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Dept;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 部门数据传输对象。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "部门DTO")
@AutoMapper(target = Dept.class)
public record DeptDTO(
        @Schema(description = "主键ID（更新时必填）") String id,
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "部门名称") String deptName,
        @Schema(description = "父部门ID") String parentId,
        @Schema(description = "部门编码") String deptCode,
        @Schema(description = "位置") String location,
        @Schema(description = "区域编码") String regionCode,
        @Schema(description = "负责人ID") String leaderId,
        @Schema(description = "负责人姓名") String leaderName,
        @Schema(description = "联系电话") String phone,
        @Schema(description = "邮箱") String email
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
