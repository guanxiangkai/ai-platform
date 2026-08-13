package com.ai.auth.log;

import com.ai.api.security.PlatformSuperAdmin;
import com.ai.auth.repository.AuthLoginLogRepository;
import com.ai.auth.properties.AuthSuperAdminProperties;
import io.github.guanxiangkai.web.plus.log.entity.BaseLog;
import io.github.guanxiangkai.web.plus.log.spi.LoginLogHandler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 将 LoginLog 切面生成的登录日志直接写入系统登录日志表。
 */
public class LoginLogHandlerImpl implements LoginLogHandler {

    private final AuthLoginLogRepository repository;
    private final AuthSuperAdminProperties superAdminProperties;

    public LoginLogHandlerImpl(
            AuthLoginLogRepository repository,
            AuthSuperAdminProperties superAdminProperties
    ) {
        this.repository = repository;
        this.superAdminProperties = superAdminProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(BaseLog entity) {
        if (!(entity instanceof AuthLoginLogRecord record)) {
            throw new IllegalArgumentException("Unsupported login log type: " + entity.getClass().getName());
        }
        prepare(record);
        repository.saveAndFlush(record);
    }

    private void prepare(AuthLoginLogRecord record) {
        LocalDateTime now = LocalDateTime.now();
        resolveLoginUserContext(record);
        record.setId(firstText(record.getId(), UUID.randomUUID().toString().replace("-", "")));
        record.setTenantId(firstText(record.getTenantId(), "0"));
        record.setLocation(firstText(record.getLocation(), ""));
        record.setLogTime(record.getLogTime() == null ? now : record.getLogTime());
        record.setCreateTime(record.getCreateTime() == null ? now : record.getCreateTime());
        record.setUpdateTime(record.getUpdateTime() == null ? now : record.getUpdateTime());
        record.setCreateBy(firstText(record.getCreateBy(), record.getUserId()));
        record.setUpdateBy(firstText(record.getUpdateBy(), record.getUserId()));
        record.setBrowser(firstText(record.getBrowser(), resolveBrowser(record.getUserAgent())));
        record.setOs(firstText(record.getOs(), resolveOs(record.getUserAgent())));
        normalizeLengths(record);
    }

    private String resolveBrowser(String userAgent) {
        if (!StringUtils.hasText(userAgent)) return null;
        if (userAgent.contains("Edg/")) return "Microsoft Edge";
        if (userAgent.contains("Chrome/")) return "Google Chrome";
        if (userAgent.contains("Firefox/")) return "Mozilla Firefox";
        if (userAgent.contains("Safari/") && userAgent.contains("Version/")) return "Safari";
        return "其他浏览器";
    }

    private String resolveOs(String userAgent) {
        if (!StringUtils.hasText(userAgent)) return null;
        if (userAgent.contains("Windows NT")) return "Windows";
        if (userAgent.contains("Mac OS X")) return "macOS";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) return "iOS";
        if (userAgent.contains("Linux")) return "Linux";
        return "其他系统";
    }

    private String firstText(String first, String fallback) {
        return StringUtils.hasText(first) ? first.trim() : fallback;
    }

    private void resolveLoginUserContext(AuthLoginLogRecord record) {
        if (isSuperAdmin(record)) {
            record.setUserId(PlatformSuperAdmin.USER_ID);
            record.setTenantId(firstText(
                    record.getTenantId(),
                    repository.findDefaultTenantId().orElse("0")
            ));
            return;
        }
        if (!StringUtils.hasText(record.getUsername())) {
            return;
        }
        repository.findUserContextByUsername(record.getUsername()).ifPresent(context -> {
            record.setUserId(firstText(record.getUserId(), context.getUserId()));
            record.setTenantId(firstText(record.getTenantId(), context.getTenantId()));
        });
    }

    private boolean isSuperAdmin(AuthLoginLogRecord record) {
        return PlatformSuperAdmin.USER_ID.equals(record.getUserId())
                || superAdminProperties.matchesUsername(record.getUsername());
    }

    private void normalizeLengths(AuthLoginLogRecord record) {
        record.setTraceId(truncate(record.getTraceId(), 64));
        record.setUserId(truncate(record.getUserId(), 64));
        record.setUsername(truncate(record.getUsername(), 64));
        record.setClientIp(truncate(record.getClientIp(), 50));
        record.setLocation(truncate(record.getLocation(), 100));
        record.setStatus(truncate(record.getStatus(), 20));
        record.setMessage(truncate(record.getMessage(), 500));
        record.setTenantId(truncate(record.getTenantId(), 64));
        record.setCreateBy(truncate(record.getCreateBy(), 64));
        record.setUpdateBy(truncate(record.getUpdateBy(), 64));
        record.setAction(truncate(record.getAction(), 50));
        record.setUserAgent(truncate(record.getUserAgent(), 500));
        record.setBrowser(truncate(record.getBrowser(), 100));
        record.setOs(truncate(record.getOs(), 100));
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
