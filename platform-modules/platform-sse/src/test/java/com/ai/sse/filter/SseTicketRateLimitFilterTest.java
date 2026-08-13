package com.ai.sse.filter;

import com.ai.sse.config.SseProperties;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SseTicketRateLimitFilterTest {

    @Test
    void rejectionShouldUseUnifiedApiResponseSerialization() throws Exception {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        SseProperties properties = mock(SseProperties.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        WebFilterChain chain = mock(WebFilterChain.class);
        byte[] serialized = "serialized-api-response".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(properties.ticketRateLimit()).thenReturn(5);
        when(properties.ticketRateWindow()).thenReturn(60_000L);
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.<String>anyList(),
                anyString()))
                .thenReturn(Flux.just(6L));
        when(objectMapper.writeValueAsBytes(any(ApiResponse.class))).thenReturn(serialized);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/sse/ticket")
                        .header(AuthConstants.HeaderConstants.USER_ID, "user-1")
                        .build());

        new SseTicketRateLimitFilter(redisTemplate, properties, objectMapper)
                .filter(exchange, chain)
                .block();

        ArgumentCaptor<ApiResponse<?>> responseCaptor = ArgumentCaptor.forClass(ApiResponse.class);
        verify(objectMapper).writeValueAsBytes(responseCaptor.capture());
        assertThat(responseCaptor.getValue().code()).isEqualTo(429);
        assertThat(responseCaptor.getValue().message()).isEqualTo("请求过于频繁，请稍后再试");
        assertThat(exchange.getResponse().getBodyAsString().block()).isEqualTo("serialized-api-response");
    }
}
