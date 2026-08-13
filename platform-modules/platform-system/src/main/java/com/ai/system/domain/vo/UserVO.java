package com.ai.system.domain.vo;

import io.github.guanxiangkai.web.plus.core.annotation.DictField;
import io.github.guanxiangkai.web.plus.core.domain.vo.DataVO;
import io.github.guanxiangkai.web.plus.core.util.StringUtils;
import com.ai.system.constants.SystemConstants;
import com.ai.system.domain.entity.User;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户详情视图对象
 * <p>
 * 继承 {@link DataVO}，复用 id / createTime / updateTime / remark / enabled / sortOrder
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户详情视图对象")
@AutoMapper(target = User.class)
public class UserVO extends DataVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别(0:未知,1:男,2:女)")
    @DictField(type = SystemConstants.DictTypeConstants.SYS_GENDER)
    private Integer gender;

    @Schema(description = "性别名称")
    private String genderLabel;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "用户类型(ADMIN-管理员,USER-用户)")
    @DictField(type = SystemConstants.DictTypeConstants.SYS_USER_TYPE)
    private String userType;

    @Schema(description = "用户类型名称")
    private String userTypeLabel;

    @Schema(description = "角色列表")
    private Set<RoleVO> roles;

    @Schema(description = "岗位")
    private Set<RoleVO> posts;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    /**
     * 返回脱敏后的手机号码，避免在详情接口暴露完整号码。
     *
     * @return 脱敏手机号码
     */
    public String getPhone() {
        return StringUtils.maskMobile(phone);
    }
}
