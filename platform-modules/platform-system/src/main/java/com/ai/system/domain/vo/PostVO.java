package com.ai.system.domain.vo;

import com.ai.system.domain.entity.Post;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 岗位详情视图对象
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "岗位VO")
@AutoMapper(target = Post.class)
public class PostVO implements Serializable {
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

    @Schema(description = "职位名称")
    private String postName;

    @Schema(description = "职位编码")
    private String postCode;

    @Schema(description = "上级岗位ID")
    private String parentId;

    @Schema(description = "排序")
    private Integer sort;
}
