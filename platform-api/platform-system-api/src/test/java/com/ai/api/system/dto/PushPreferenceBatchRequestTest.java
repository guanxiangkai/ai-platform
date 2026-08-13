package com.ai.api.system.dto;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushPreferenceBatchRequestTest {

    @Test
    void requestShouldNormalizeUserIds() {
        PushPreferenceBatchRequest request = new PushPreferenceBatchRequest(
                java.util.Arrays.asList(" user-1 ", "", null, "user-1", "user-2"));

        assertThat(request.userIds()).containsExactly("user-1", "user-2");
    }

    @Test
    void requestShouldRejectOversizedBatch() {
        var userIds = IntStream.rangeClosed(0, PushPreferenceBatchRequest.MAX_USER_IDS)
                .mapToObj(index -> "user-" + index)
                .toList();

        assertThatThrownBy(() -> new PushPreferenceBatchRequest(userIds))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(PushPreferenceBatchRequest.MAX_USER_IDS));
    }
}
