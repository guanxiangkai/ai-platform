package com.ai.system.domain.vo;

import io.github.guanxiangkai.web.plus.core.domain.vo.DataPageVO;
import com.ai.system.domain.entity.Register;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;
import com.ai.system.domain.RegisterState;

/**
 * 注册记录分页列表视图对象
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "注册记录分页VO")
@AutoMapper(target = Register.class)
public class RegisterPageVO extends DataPageVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "系统生成的用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "平台部门ID")
    private String deptId;

    @Schema(description = "租户外部档案目录主体ID")
    private String directorySubjectId;

    @Schema(description = "注册开通状态")
    private RegisterState registrationState;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    @Schema(description = "审核人ID")
    private String auditBy;

    @Schema(description = "本地系统账户ID(审核通过后生成)")
    private String userId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
