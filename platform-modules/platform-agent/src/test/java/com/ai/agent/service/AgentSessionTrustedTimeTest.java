package com.ai.agent.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 平台可信时间上下文回归测试。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
class AgentSessionTrustedTimeTest {

    @Test
    void shouldPrependTrustedTimeAndPreserveOriginalMessageBoundary() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-26T02:15:30Z"), ZoneId.of("Asia/Shanghai"));

        String message = AgentSessionService.trustedMessage("  查询本周计划  ", clock);

        assertThat(message).isEqualTo("""
                【平台可信时间】
                当前时间：2026-08-26T10:15:30+08:00[Asia/Shanghai]
                当前日期：2026-08-26
                本周周一至周日：2026-08-24 至 2026-08-30
                下周周一至周日：2026-08-31 至 2026-09-06
                【用户原始消息】
                查询本周计划""");
    }

    @Test
    void shouldCalculateWeeksAcrossYearBoundary() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-12-31T16:30:00Z"), ZoneId.of("Asia/Shanghai"));

        String message = AgentSessionService.trustedMessage("继续", clock);

        assertThat(message)
                .contains("当前日期：2027-01-01")
                .contains("本周周一至周日：2026-12-28 至 2027-01-03")
                .contains("下周周一至周日：2027-01-04 至 2027-01-10")
                .endsWith("【用户原始消息】\n继续");
    }
}
