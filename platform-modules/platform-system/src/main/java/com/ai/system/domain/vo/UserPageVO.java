package com.ai.system.domain.vo;

import io.github.guanxiangkai.web.plus.core.domain.vo.BasePageVO;
import io.github.guanxiangkai.web.plus.core.util.StringUtils;
import com.ai.system.domain.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户分页列表视图对象
 * <p>
 * 继承 {@link BasePageVO}，复用主键字段。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户分页VO")
@AutoMapper(target = User.class)
public class UserPageVO extends BasePageVO {

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

    @Schema(description = "角色名称列表")
    private List<String> roleNames;

    @Schema(description = "岗位名称列表")
    private List<String> postNames;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 返回脱敏后的手机号码，避免在列表接口暴露完整号码。
     *
     * @return 脱敏手机号码
     */
    public String getPhone() {
        return StringUtils.maskMobile(phone);
    }
}
