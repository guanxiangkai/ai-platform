package com.ai.system.domain.entity;

import io.github.guanxiangkai.jpa.plus.field.desensitize.annotation.Desensitize;
import io.github.guanxiangkai.jpa.plus.field.desensitize.annotation.DesensitizeStrategy;
import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import com.ai.system.constants.SystemConstants;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 用户实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, exclude = {"userRoles", "userPosts"})
@Entity
@Table(name = "sys_user", comment = "用户表")
public class User extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "username", nullable = false, length = 64, comment = "用户名")
    private String username;

    @Column(name = "password", nullable = false, length = 255, comment = "密码")
    private String password;

    @Column(name = "nickname", length = 64, comment = "昵称")
    private String nickname;

    @Column(name = "real_name", length = 64, comment = "真实姓名")
    private String realName;

    @Column(name = "email", length = 128, comment = "邮箱")
    private String email;

    @Desensitize(strategy = DesensitizeStrategy.PHONE)
    @Column(name = "phone", length = 11, comment = "手机号")
    private String phone;

    @Column(name = "gender", comment = "性别(0-未知,1-男,2-女)")
    private Integer gender;

    @Column(name = "avatar", length = 500, comment = "头像URL")
    private String avatar;

    @Column(name = "dept_id", length = 64, comment = "部门ID")
    private String deptId;

    @Column(
            name = "user_type",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20) default 'USER'",
            comment = "用户类型(ADMIN-管理员,USER-用户)"
    )
    private String userType = SystemConstants.UserConstants.TYPE_USER;

    @Column(name = "last_login_ip", length = 17, comment = "最后登录IP")
    private String lastLoginIp;

    @Column(name = "last_login_time", comment = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @ToString.Exclude
    private Set<UserRole> userRoles = new HashSet<>();

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @ToString.Exclude
    private Set<UserPost> userPosts = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void normalizeAdminFields() {
        if (userType == null || userType.isBlank()) {
            userType = SystemConstants.UserConstants.TYPE_USER;
        } else {
            userType = userType.trim().toUpperCase(Locale.ROOT);
        }
    }
}
