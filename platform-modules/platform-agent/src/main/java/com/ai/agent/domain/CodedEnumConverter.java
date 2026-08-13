package com.ai.agent.domain;

import jakarta.persistence.AttributeConverter;

/**
 * 将带稳定代码的枚举持久化为字符串。
 *
 * @param <E> 枚举类型
 * @since 1.0.0
 */
public abstract class CodedEnumConverter<E extends Enum<E> & CodedEnum>
        implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected CodedEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        return CodedEnum.optional(type, dbData);
    }
}
