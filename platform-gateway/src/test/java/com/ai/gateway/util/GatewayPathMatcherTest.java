package com.ai.gateway.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayPathMatcherTest {

    @Test
    void matchesExactSingleSegmentAndTrailingWildcardPatterns() {
        List<String> patterns = List.of(
                "/auth/login",
                "/agent/session/ask",
                "/actuator/**"
        );

        assertThat(GatewayPathMatcher.matchesAny(patterns, "/auth/login")).isTrue();
        assertThat(GatewayPathMatcher.matchesAny(patterns, "/agent/session/ask")).isTrue();
        assertThat(GatewayPathMatcher.matchesAny(patterns, "/actuator/health/readiness")).isTrue();
        assertThat(GatewayPathMatcher.matchesAny(patterns, "/agent/session/history")).isFalse();
    }

    @Test
    void ignoresBlankAndInvalidPatterns() {
        List<String> patterns = List.of("", "/api/**/internal");

        assertThat(GatewayPathMatcher.matchesAny(patterns, "/api/user/internal")).isFalse();
    }
}
