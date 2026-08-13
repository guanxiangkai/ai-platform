package com.ai.api.system.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserIdentityBatchRequestTest {

    @Test
    void shouldNormalizeAndDeduplicateUsernames() {
        UserIdentityBatchRequest request = new UserIdentityBatchRequest(
                List.of(" user ", "admin", "user", " "));

        assertEquals(List.of("user", "admin"), request.usernames());
    }

    @Test
    void shouldRejectMoreThanFiveHundredUsernames() {
        List<String> usernames = IntStream.rangeClosed(1, 501)
                .mapToObj(index -> "user" + index)
                .toList();

        assertThrows(IllegalArgumentException.class, () -> new UserIdentityBatchRequest(usernames));
    }
}
