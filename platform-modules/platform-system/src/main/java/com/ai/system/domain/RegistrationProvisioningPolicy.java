package com.ai.system.domain;

/**
 * 注册开通错误的领域规范。
 *
 * <p>错误文本会同时写入注册记录和出站事件，因此在进入持久化边界前统一清理并限制长度。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class RegistrationProvisioningPolicy {

    /** 持久化错误文本的最大字符数。 */
    public static final int ERROR_MAX_LENGTH = 500;

    /** 上游未提供有效原因时的错误文本。 */
    public static final String DEFAULT_ERROR_MESSAGE = "开通失败";

    private RegistrationProvisioningPolicy() {
    }

    /**
     * 将上游异常文本收敛为可持久化的错误信息。
     *
     * @param reason 上游错误原因
     * @return 非空且长度受限的错误文本
     */
    public static String normalizeError(String reason) {
        String normalized = reason == null || reason.isBlank()
                ? DEFAULT_ERROR_MESSAGE
                : reason.trim();
        return normalized.length() <= ERROR_MAX_LENGTH
                ? normalized
                : normalized.substring(0, ERROR_MAX_LENGTH);
    }
}
