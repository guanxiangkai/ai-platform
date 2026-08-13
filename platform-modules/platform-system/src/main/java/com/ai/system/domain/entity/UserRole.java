package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;

/**
 * 用户角色关系实体
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
@EqualsAndHashCode(callSuper = true, exclude = {"user"})
@Entity
@Table(name = "sys_user_role", comment = "用户角色关系表")
public class UserRole extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 64, comment = "用户ID")
    private String userId;

    /**
     * 角色ID
     */
    @Column(name = "role_id", nullable = false, length = 64, comment = "角色ID")
    private String roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false, comment = "用户ID")
    @ToString.Exclude
    private User user;
}
