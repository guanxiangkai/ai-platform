package com.ai.auth.constants;

/**
 * Auth 服务常量
 */
public final class AuthModuleConstants {

    private AuthModuleConstants() {
        throw new UnsupportedOperationException("这是一个效用类，无法实例化");
    }

    /**
     * Redis key 常量
     */
    public static final class RedisKeyConstants {

        public static final String LOGIN_FAIL_PREFIX = "security:auth:login:fail:";
        public static final String LOGIN_LOCK_PREFIX = "security:auth:login:lock:";
        public static final String REFRESH_RATE_PREFIX = "security:auth:refresh:rate:";

        private RedisKeyConstants() {
        }
    }

}
