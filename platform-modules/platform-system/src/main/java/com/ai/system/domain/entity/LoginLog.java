package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.log.entity.BaseLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 登录日志实体。
 *
 * <p>继承 {@link BaseLog} 复用租户、审计与日志公共字段。认证服务通过
 * {@code LoginLogHandler} SPI 写入，系统服务负责管理端查询与保留策略。</p>
 *
 * <h3>aspect 自动填充字段</h3>
 * <ul>
 *   <li>BaseLog 公共字段：traceId / userId / username / clientIp / location / status / message / logTime</li>
 *   <li>{@code action}   — 来自 {@code @LoginLog#action()}，如 "LOGIN" / "LOGOUT"</li>
 *   <li>{@code userAgent} — 请求 User-Agent</li>
 * </ul>
 * <h3>应用手动填充字段</h3>
 * <ul>
 *   <li>{@code browser} / {@code os} — 可在 {@code LoginLogHandlerImpl} 中通过 UA 解析库填充</li>
 * </ul>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_login_log", comment = "登录日志表", indexes = {
        @Index(name = "idx_login_log_username", columnList = "username"),
        @Index(name = "idx_login_log_time", columnList = "log_time")
})
public class LoginLog extends BaseLog {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录动作（如 LOGIN / LOGOUT / REFRESH_TOKEN）
     * <p>由 {@code LoginLogAspect} 从 {@code @LoginLog#action()} 注入</p>
     */
    @Column(name = "action", length = 50, comment = "登录动作")
    private String action;

    /**
     * 客户端 User-Agent（原始 Header 值）
     * <p>由 {@code LoginLogAspect} 自动注入</p>
     */
    @Column(name = "user_agent", length = 500, comment = "客户端代理信息")
    private String userAgent;

    /**
     * 解析后的浏览器名称（如 Chrome 124、Safari 17）
     * <p>可在 {@code LoginLogHandlerImpl} 中通过 UA 解析库填充</p>
     */
    @Column(name = "browser", length = 100, comment = "浏览器")
    private String browser;

    /**
     * 解析后的操作系统（如 Windows 11、macOS Sequoia、iOS 17）
     * <p>可在 {@code LoginLogHandlerImpl} 中通过 UA 解析库填充</p>
     */
    @Column(name = "os", length = 100, comment = "操作系统")
    private String os;

}
