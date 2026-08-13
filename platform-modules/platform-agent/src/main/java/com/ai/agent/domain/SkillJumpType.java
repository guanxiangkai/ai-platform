package com.ai.agent.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Converter;

/**
 * 技能跳转类型。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum SkillJumpType implements CodedEnum {
    INTERNAL("internal"), EXTERNAL("external");

    private final String code;

    SkillJumpType(String code) { this.code = code; }

    @Override
    @JsonValue
    public String code() { return code; }

    public boolean isInternal() { return this == INTERNAL; }

    @JsonCreator
    public static SkillJumpType fromCode(String code) { return CodedEnum.required(SkillJumpType.class, code); }

    @Converter
    public static final class JpaConverter extends CodedEnumConverter<SkillJumpType> {
        public JpaConverter() { super(SkillJumpType.class); }
    }
}
