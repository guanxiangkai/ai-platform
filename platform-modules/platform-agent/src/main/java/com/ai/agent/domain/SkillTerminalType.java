package com.ai.agent.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Converter;

/**
 * 技能展示终端。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum SkillTerminalType implements CodedEnum {
    WEBSITE("website"), MANAGEMENT("management");

    private final String code;

    SkillTerminalType(String code) { this.code = code; }

    @Override
    @JsonValue
    public String code() { return code; }

    public boolean isWebsite() { return this == WEBSITE; }

    @JsonCreator
    public static SkillTerminalType fromCode(String code) {
        return CodedEnum.required(SkillTerminalType.class, code);
    }

    @Converter
    public static final class JpaConverter extends CodedEnumConverter<SkillTerminalType> {
        public JpaConverter() { super(SkillTerminalType.class); }
    }
}
