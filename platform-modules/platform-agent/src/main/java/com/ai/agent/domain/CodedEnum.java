package com.ai.agent.domain;

import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 使用稳定外部代码的枚举契约。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface CodedEnum {
    /** 返回 API 与数据库共用的稳定代码。 */
    String code();

    /** 按稳定代码解析必填枚举值。 */
    static <E extends Enum<E> & CodedEnum> E required(Class<E> type, String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException(type.getSimpleName() + " 代码不能为空");
        }
        return Arrays.stream(type.getEnumConstants())
                .filter(value -> value.code().equals(code.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(type.getSimpleName() + " 代码无效: " + code));
    }

    /** 按稳定代码解析可选枚举值。 */
    static <E extends Enum<E> & CodedEnum> E optional(Class<E> type, String code) {
        return StringUtils.hasText(code) ? required(type, code) : null;
    }
}
