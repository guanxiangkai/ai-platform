package com.ai.system.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动后重建 auth 服务登录依赖的用户认证缓存。
 */
@Component
@RequiredArgsConstructor
public class AuthUserCacheWarmup {

    private final AuthUserCacheService authUserCacheService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        authUserCacheService.refreshAll();
    }
}
