package com.ai.gateway.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReactiveResponseUtilsTest {

    @Test
    void committedResponseIsNotMutated() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        when(exchange.getResponse()).thenReturn(response);
        when(response.isCommitted()).thenReturn(true);

        ReactiveResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "请先登录").block();

        verify(response, never()).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response, never()).getHeaders();
    }
}
