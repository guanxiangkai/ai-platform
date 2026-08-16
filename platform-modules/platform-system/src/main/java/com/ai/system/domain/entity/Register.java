package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import com.ai.system.domain.RegistrationProvisioningPolicy;
import com.ai.system.domain.RegisterState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 用户注册记录实体
 * <p>
 * 用户自助注册后生成注册记录：
 * <ul>
 *   <li>注册时：系统根据真实姓名拼音首字母自动生成用户名，并从业务服务匹配员工档案。</li>
 *   <li>匹配失败（未找到员工或员工已有账户）则注册被拒绝。</li>
 *   <li>审核通过时：仅创建用户账户，并回写 userId，同时通知业务服务绑定员工账户。</li>
 * </ul>
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
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_register", comment = "用户注册记录表")
public class Register extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 自动生成的用户名（注册提交后返回给用户，让用户记住）
     */
    @Column(name = "username", length = 64, comment = "用户名(系统根据真实姓名自动生成)")
    private String username;

    @Column(name = "password", nullable = false, length = 255, comment = "密码(加密存储)")
    private String password;

    @Column(name = "nickname", length = 64, comment = "昵称")
    private String nickname;

    @Column(name = "real_name", nullable = false, length = 64, comment = "真实姓名")
    private String realName;

    @Column(name = "email", length = 128, comment = "邮箱")
    private String email;

    @Column(name = "phone", length = 11, comment = "手机号")
    private String phone;

    @Column(name = "gender", length = 20, comment = "性别(0-未知,1-男,2-女)")
    private String gender;

    /**
     * 注册时选择的部门ID
     */
    @Column(name = "dept_id", nullable = false, length = 64, comment = "注册部门ID")
    private String deptId;

    /**
     * 注册时匹配到的员工档案ID（对应 business 模块 Personnel 表）
     */
    @Column(name = "personnel_id", nullable = false, length = 64, comment = "匹配员工档案ID")
    private String personnelId;

    /** 注册开通状态；仅 ACTIVE 的账户可登录。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_state", nullable = false, length = 32, comment = "注册开通状态")
    @Builder.Default
    private RegisterState registrationState = RegisterState.PENDING;

    @Column(name = "audit_remark", length = 500, comment = "审核备注")
    private String auditRemark;

    @Column(name = "audit_time", comment = "审核时间")
    private LocalDateTime auditTime;

    @Column(name = "audit_by", length = 64, comment = "审核人ID")
    private String auditBy;

    /**
     * 审核通过后回填的用户ID
     */
    @Column(name = "user_id", length = 64, comment = "关联用户ID(审核通过后自动创建并回填)")
    private String userId;

    @Column(name = "provisioning_error", length = RegistrationProvisioningPolicy.ERROR_MAX_LENGTH,
            comment = "开通失败原因")
    private String provisioningError;
}
