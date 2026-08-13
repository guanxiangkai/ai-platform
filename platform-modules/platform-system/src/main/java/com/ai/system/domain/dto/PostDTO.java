package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Post;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 岗位更新数据传输对象。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "岗位DTO")
@AutoMapper(target = Post.class)
public record PostDTO(
        @Schema(description = "主键ID（更新时必填）") String id,
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @Schema(description = "岗位名称") String postName,
        @Schema(description = "岗位编码") String postCode,
        @Schema(description = "上级岗位ID") String parentId,
        @Schema(description = "排序") Integer sort
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
