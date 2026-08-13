package com.ai.system.domain.dto;

import com.ai.system.domain.entity.Setting;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 应用设置数据传输对象
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Schema(description = "应用设置DTO")
@AutoMapper(target = Setting.class)
public record SettingDTO(
        @Schema(description = "语言") @Size(max = 32, message = "语言标识不能超过32个字符") String language,
        @Schema(description = "主题") @Pattern(regexp = "light|dark|system", message = "主题必须是light、dark或system") String theme,
        @Schema(description = "字体大小") @Min(value = 12, message = "字体大小不能小于12") @Max(value = 24, message = "字体大小不能大于24") Integer fontSize,
        @Schema(description = "桌面通知") Boolean desktopNotification,
        @Schema(description = "声音提示") Boolean soundNotification,
        @Schema(description = "邮件通知") Boolean emailNotification,
        @Schema(description = "通知频率") @Pattern(regexp = "realtime|daily|weekly", message = "通知频率必须是realtime、daily或weekly") String notificationFrequency,
        @Schema(description = "自动保存") Boolean autoSave,
        @Schema(description = "登录保护") Boolean loginProtection,
        @Schema(description = "会话超时(分钟)") @Min(value = 5, message = "会话超时不能小于5分钟") @Max(value = 1440, message = "会话超时不能大于1440分钟") Integer sessionTimeout,
        @Schema(description = "按产品命名空间隔离的扩展偏好") @Size(max = 50, message = "扩展命名空间不能超过50个") Map<@Size(max = 128, message = "扩展命名空间不能超过128个字符") String, Object> extensions
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
