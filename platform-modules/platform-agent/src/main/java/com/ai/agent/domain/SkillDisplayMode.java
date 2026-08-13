package com.ai.agent.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Converter;

/**
 * 技能展示模式。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum SkillDisplayMode implements CodedEnum {
    PROMPT("prompt"), CHAT("chat"), LIST("list"), JUMP("jump");

    private final String code;

    SkillDisplayMode(String code) { this.code = code; }

    @Override
    @JsonValue
    public String code() { return code; }

    @JsonCreator
    public static SkillDisplayMode fromCode(String code) {
        return CodedEnum.required(SkillDisplayMode.class, code);
    }

    @Converter
    public static final class JpaConverter extends CodedEnumConverter<SkillDisplayMode> {
        public JpaConverter() { super(SkillDisplayMode.class); }
    }
}
