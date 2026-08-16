package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 应用设置实体
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
@Table(name = "sys_setting", comment = "应用设置表")
@Schema(description = "应用设置")
public class Setting extends DataTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "所属人ID")
    @Column(name = "owner_id", length = 64, comment = "所属人ID")
    private String ownerId;

    @Schema(description = "语言")
    @Column(name = "language", length = 32, comment = "语言")
    private String language;

    @Schema(description = "主题")
    @Column(name = "theme", length = 32, comment = "主题")
    private String theme;

    @Schema(description = "字体大小")
    @Column(name = "font_size", comment = "字体大小")
    private Integer fontSize;

    @Schema(description = "桌面通知")
    @Column(name = "desktop_notification", comment = "桌面通知")
    private Boolean desktopNotification;

    @Schema(description = "声音提示")
    @Column(name = "sound_notification", comment = "声音提示")
    private Boolean soundNotification;

    @Schema(description = "邮件通知")
    @Column(name = "email_notification", comment = "邮件通知")
    private Boolean emailNotification;

    @Schema(description = "通知频率")
    @Column(name = "notification_frequency", length = 32, comment = "通知频率")
    private String notificationFrequency;

    @Schema(description = "自动保存")
    @Column(name = "auto_save", comment = "自动保存")
    private Boolean autoSave;

    @Schema(description = "登录保护")
    @Column(name = "login_protection", comment = "登录保护")
    private Boolean loginProtection;

    @Schema(description = "会话超时(分钟)")
    @Column(name = "session_timeout", comment = "会话超时(分钟)")
    private Integer sessionTimeout;

    /**
     * 产品命名空间扩展偏好。
     *
     * <p>平台只负责按当前租户和用户保存完整命名空间对象，不解释产品内部字段。例如产品工作台使用
     * {@code product.workbench}，其他产品应使用自己的命名空间，禁止把产品字段重新增加到本实体。</p>
     */
    @Schema(description = "按产品命名空间隔离的扩展偏好")
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extensions", nullable = false, columnDefinition = "jsonb", comment = "产品命名空间扩展偏好")
    private Map<String, Object> extensions = new LinkedHashMap<>();
}
