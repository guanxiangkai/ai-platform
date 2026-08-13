package com.ai.system.domain.vo;

import com.ai.system.domain.entity.Setting;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 当前用户的通用设置视图。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "应用设置详情视图对象")
@AutoMapper(target = Setting.class)
public class SettingVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "语言")
    private String language;

    @Schema(description = "主题")
    private String theme;

    @Schema(description = "字体大小")
    private Integer fontSize;

    @Schema(description = "桌面通知")
    private Boolean desktopNotification;

    @Schema(description = "声音提示")
    private Boolean soundNotification;

    @Schema(description = "邮件通知")
    private Boolean emailNotification;

    @Schema(description = "通知频率")
    private String notificationFrequency;

    @Schema(description = "自动保存")
    private Boolean autoSave;

    @Schema(description = "登录保护")
    private Boolean loginProtection;

    @Schema(description = "会话超时(分钟)")
    private Integer sessionTimeout;

    @Schema(description = "按产品命名空间隔离的扩展偏好")
    private Map<String, Object> extensions;

}
