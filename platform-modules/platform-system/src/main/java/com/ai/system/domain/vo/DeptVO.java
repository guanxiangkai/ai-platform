package com.ai.system.domain.vo;

import io.github.guanxiangkai.web.plus.core.util.StringUtils;
import com.ai.system.domain.entity.Dept;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门详情视图对象
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "部门VO")
@AutoMapper(target = Dept.class)
public class DeptVO implements Serializable {
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

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "父部门ID")
    private String parentId;

    @Schema(description = "部门编码")
    private String deptCode;

    @Schema(description = "位置")
    private String location;

    @Schema(description = "区域编码")
    private String regionCode;

    @Schema(description = "负责人ID")
    private String leaderId;

    @Schema(description = "负责人姓名")
    private String leaderName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "祖级列表")
    private String ancestors;

    @Schema(description = "子部门列表（树形结构使用）")
    private List<DeptVO> children;
    /**
     * 返回脱敏后的联系电话，避免在详情接口暴露完整号码。
     *
     * @return 脱敏联系电话
     */
    public String getPhone() {
        return StringUtils.maskMobile(phone);
    }
}
