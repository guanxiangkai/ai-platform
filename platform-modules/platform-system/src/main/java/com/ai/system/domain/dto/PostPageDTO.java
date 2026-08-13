package com.ai.system.domain.dto;

import io.github.guanxiangkai.web.plus.core.model.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 岗位分页查询参数
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "岗位分页查询参数")
public class PostPageDTO extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "岗位名称（模糊查询）")
    private String postName;

    @Schema(description = "岗位编码（模糊查询）")
    private String postCode;

    @Schema(description = "上级岗位ID")
    private String parentId;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
