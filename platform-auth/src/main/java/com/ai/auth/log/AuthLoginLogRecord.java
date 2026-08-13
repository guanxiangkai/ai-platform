package com.ai.auth.log;

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
 * Auth 登录日志实体，由 LoginLog 切面填充后直接写入系统登录日志表。
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_login_log", comment = "登录日志表", indexes = {
        @Index(name = "idx_login_log_username", columnList = "username"),
        @Index(name = "idx_login_log_time", columnList = "log_time")
})
public class AuthLoginLogRecord extends BaseLog {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "action", length = 50, comment = "登录动作")
    private String action;

    @Column(name = "user_agent", length = 500, comment = "客户端代理信息")
    private String userAgent;

    @Column(name = "browser", length = 100, comment = "浏览器")
    private String browser;

    @Column(name = "os", length = 100, comment = "操作系统")
    private String os;
}
