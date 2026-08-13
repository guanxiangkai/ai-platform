package com.ai.scheduler.integration;

import com.ai.scheduler.config.SchedulerProperties;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tech.powerjob.client.PowerJobClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 按应用延迟创建并复用 PowerJob OpenAPI 客户端。 */
@Component
@RequiredArgsConstructor
public class PowerJobClientRegistry {

    private final SchedulerProperties properties;
    private final Map<String, PowerJobClient> clients = new ConcurrentHashMap<>();

    /** 返回指定逻辑应用的已认证客户端。 */
    public PowerJobClient client(String applicationCode, SchedulerProperties.Application application) {
        if (!StringUtils.hasText(applicationCode) || application == null) {
            throw new BizException("调度应用配置不存在");
        }
        List<String> addresses = properties.getServerAddresses().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (addresses.isEmpty()) {
            throw new BizException("PowerJob Server 地址未配置");
        }
        if (!StringUtils.hasText(application.getAppName()) || !StringUtils.hasText(application.getPassword())) {
            throw new BizException("PowerJob 应用名称或访问凭据未配置");
        }
        String cacheKey = applicationCode + "@" + application.getAppName().trim();
        return clients.computeIfAbsent(cacheKey, ignored -> new PowerJobClient(
                addresses, application.getAppName().trim(), application.getPassword()));
    }

    /** 关闭底层 HTTP 连接池。 */
    @PreDestroy
    public void close() {
        clients.values().forEach(client -> {
            try {
                client.close();
            } catch (IOException ignored) {
                // 容器退出时不因连接池关闭失败阻断 Spring 生命周期。
            }
        });
        clients.clear();
    }
}
