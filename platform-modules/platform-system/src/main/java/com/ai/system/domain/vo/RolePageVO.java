package com.ai.system.domain.vo;

import io.github.guanxiangkai.web.plus.core.domain.vo.DataPageVO;
import com.ai.system.domain.entity.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 角色分页列表视图对象
 * <p>
 * 继承 {@link DataPageVO}，复用 id / enabled / sortOrder
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色分页VO")
@AutoMapper(target = Role.class)
public class RolePageVO extends DataPageVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否为默认注册角色")
    private Boolean defaultRegistrationRole;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
