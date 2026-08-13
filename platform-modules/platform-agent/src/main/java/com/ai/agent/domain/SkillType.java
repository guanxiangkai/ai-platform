package com.ai.agent.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Converter;

/**
 * 技能来源类型。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum SkillType implements CodedEnum {
    SYSTEM("system"), GROUP("group"), USER("user");

    private final String code;

    SkillType(String code) { this.code = code; }

    @Override
    @JsonValue
    public String code() { return code; }

    @JsonCreator
    public static SkillType fromCode(String code) { return CodedEnum.required(SkillType.class, code); }

    @Converter
    public static final class JpaConverter extends CodedEnumConverter<SkillType> {
        public JpaConverter() { super(SkillType.class); }
    }
}
