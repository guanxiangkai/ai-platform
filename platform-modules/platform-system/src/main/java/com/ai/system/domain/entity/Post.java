package com.ai.system.domain.entity;

import io.github.guanxiangkai.jpa.plus.interceptor.permission.enums.DataScopeType;
import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 岗位实体
 * <p>
 * 岗位关联部门，定义用户在该部门的数据访问权限
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_post", comment = "岗位表")
public class Post extends SortableTenantEntity {

    /**
     * 岗位名称
     */
    @Column(name = "post_name", nullable = false, length = 50, comment = "岗位名称")
    private String postName;

    /**
     * 岗位编码
     */
    @Column(name = "post_code", length = 50, comment = "岗位编码")
    private String postCode;

    /**
     * 上级岗位ID
     */
    @Column(name = "parent_id", length = 64, comment = "上级岗位ID")
    private String parentId;

    /**
     * 数据权限范围
     * <p>
     * 控制用户在该岗位下可以选择哪些部门：
     * - ALL: 可以选择所有部门
     * - DEPT: 只能选择该岗位所在的部门
     * - DEPT_AND_CHILD: 可以选择该岗位所在部门及其下级部门
     * - SELF: 只能选择自己创建的部门
     * - CUSTOM: 由数据权限处理器自定义控制
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_scope", length = 32, comment = "数据权限范围")
    private DataScopeType dataScope;

    /**
     * 部门ID
     * <p>
     * 岗位所属部门，作为数据权限的基准部门
     * </p>
     */
    @Column(name = "dept_id", updatable = false, length = 64, comment = "部门ID")
    private String deptId;

    /**
     * 上级岗位
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "id", insertable = false, updatable = false, comment = "上级岗位ID")
    @ToString.Exclude
    private Post parent;

    /**
     * 下级岗位列表
     */
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private Set<Post> children = new HashSet<>();
}
