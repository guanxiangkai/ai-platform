package com.ai.gateway.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayExceptionHandlerTest {

    private final GatewayExceptionHandler handler = new GatewayExceptionHandler();

    @Test
    void doesNotExposeResponseStatusReason() {
        MockServerWebExchange exchange = exchange();
        String internalReason = "upstream=node-a, credential=should-not-leak";

        handler.handle(exchange, new ResponseStatusException(
                HttpStatus.BAD_REQUEST, internalReason)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("请求无效")
                .doesNotContain(internalReason);
    }

    @Test
    void mapsNestedConnectionFailureWithoutInspectingExceptionMessage() {
        MockServerWebExchange exchange = exchange();

        handler.handle(exchange, new IllegalStateException(
                "opaque", new ConnectException("connection details"))).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("服务暂不可用")
                .doesNotContain("connection details");
    }

    private static MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/platform/resource").build());
    }
}
