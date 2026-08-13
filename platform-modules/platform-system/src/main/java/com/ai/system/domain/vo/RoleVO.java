package com.ai.system.domain.vo;

import io.github.guanxiangkai.web.plus.core.annotation.DictField;
import io.github.guanxiangkai.web.plus.core.domain.vo.DataVO;
import com.ai.system.constants.SystemConstants;
import com.ai.system.domain.entity.Role;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 角色视图对象
 * <p>
 * 继承 {@link DataVO}，复用 id / createTime / updateTime / remark / status / enabled / sortOrder
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色VO")
@AutoMapper(target = Role.class)
public class RoleVO extends DataVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "数据权限范围(1:全部,2:自定义,3:本部门,4:本部门及以下,5:仅本人)")
    @DictField(type = SystemConstants.DictTypeConstants.SYS_DATA_SCOPE)
    private String dataScope;

    @Schema(description = "数据权限范围名称")
    private String dataScopeLabel;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否为默认注册角色(注册审核兜底使用)")
    private Boolean defaultRegistrationRole;
}
