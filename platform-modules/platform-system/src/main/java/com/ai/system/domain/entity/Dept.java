package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 部门实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_dept", comment = "部门表")
public class Dept extends SortableTenantEntity {

    /**
     * 部门名称
     */
    @Column(name = "dept_name", nullable = false, length = 50, comment = "部门名称")
    private String deptName;

    /**
     * 父部门ID
     */
    @Column(name = "parent_id", length = 64, comment = "父部门ID")
    private String parentId;

    /**
     * 部门编码
     */
    @Column(name = "dept_code", length = 50, comment = "部门编码")
    private String deptCode;

    /**
     * 位置
     */
    @Column(name = "location", length = 200, comment = "位置")
    private String location;

    /**
     * 区域编码
     */
    @Column(name = "region_code", length = 20, comment = "区域编码")
    private String regionCode;

    /**
     * 关联区域
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_code", referencedColumnName = "region_code", insertable = false, updatable = false, comment = "区域编码")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Region region;

    /**
     * 负责人ID
     */
    @Column(name = "leader_id", length = 64, comment = "负责人ID")
    private String leaderId;

    /**
     * 负责人姓名
     */
    @Column(name = "leader_name", length = 50, comment = "负责人姓名")
    private String leaderName;

    /**
     * 联系电话
     */
    @Column(name = "phone", length = 20, comment = "联系电话")
    private String phone;

    /**
     * 邮箱
     */
    @Column(name = "email", length = 100, comment = "邮箱")
    private String email;

    /**
     * 祖级列表（逗号分隔的ID）
     */
    @Column(name = "ancestors", length = 500, comment = "祖级列表")
    private String ancestors;

    /**
     * 是否有子部门
     */
    @Column(name = "has_child", comment = "是否有子部门")
    private Boolean hasChild;
}
