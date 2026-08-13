package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Post;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/**
 * 岗位DTO创建DTO
 * <p>
 * 用于创建接口入参，包含参数校验；更新接口使用 {@link PostDTO}
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "岗位创建DTO")
@AutoMapper(target = Post.class)
public record PostCreateDTO(
        @Schema(description = "备注") String remark,
        @Schema(description = "排序号") Integer sortOrder,
        @NotBlank(message = "岗位名称不能为空") @Schema(description = "岗位名称") String postName,
        @Schema(description = "岗位编码") String postCode,
        @Schema(description = "上级岗位ID") String parentId,
        @Schema(description = "排序") Integer sort
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
