package com.ai.agent.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Converter;

/**
 * 技能动作类型。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum SkillActionType implements CodedEnum {
    PRESET_PROMPT("presetPrompt"),
    QA_CONTINUE("qaContinue"),
    QA_NO_REPEAT("qaNoRepeat"),
    JUMP("jump"),
    DATA_LIST("dataList");

    private final String code;

    SkillActionType(String code) { this.code = code; }

    @Override
    @JsonValue
    public String code() { return code; }

    public boolean isJump() { return this == JUMP; }

    @JsonCreator
    public static SkillActionType fromCode(String code) {
        return CodedEnum.required(SkillActionType.class, code);
    }

    @Converter
    public static final class JpaConverter extends CodedEnumConverter<SkillActionType> {
        public JpaConverter() { super(SkillActionType.class); }
    }
}
