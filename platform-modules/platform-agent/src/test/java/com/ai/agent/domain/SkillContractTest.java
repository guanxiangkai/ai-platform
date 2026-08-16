package com.ai.agent.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 技能稳定代码与行为契约测试。 */
class SkillContractTest {

    @Test
    void shouldResolveStableExternalCodes() {
        assertThat(SkillType.fromCode("dept")).isEqualTo(SkillType.DEPARTMENT);
        assertThat(SkillTerminalType.fromCode("website").isWebsite()).isTrue();
        assertThat(SkillActionType.fromCode("jump").isJump()).isTrue();
        assertThat(SkillJumpType.fromCode("internal").isInternal()).isTrue();
        assertThat(SkillDisplayMode.fromCode("chat")).isEqualTo(SkillDisplayMode.CHAT);
    }

    @Test
    void shouldRejectUnknownCodes() {
        assertThatThrownBy(() -> SkillActionType.fromCode("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
