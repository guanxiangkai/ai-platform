package com.ai.gateway.filter;

import io.github.guanxiangkai.web.plus.core.constant.WebPlusConstants;
import io.github.guanxiangkai.web.plus.log.filter.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestLogFilterTest {

    @Test
    void preservesTraceIdEstablishedByWebPlusEntryFilter() {
        RequestLogFilter filter = new RequestLogFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/system/users")
                .header(WebPlusConstants.TRACE_ID_HEADER, "trace-gateway"));
        AtomicReference<String> forwardedTraceId = new AtomicReference<>();

        filter.filter(exchange, forwardedExchange -> {
            forwardedTraceId.set(forwardedExchange.getRequest().getHeaders()
                    .getFirst(WebPlusConstants.TRACE_ID_HEADER));
            return Mono.empty();
        }).block();

        assertThat(forwardedTraceId).hasValue("trace-gateway");
    }

    @Test
    void keepsTraceIdOnErrorPath() {
        RequestLogFilter filter = new RequestLogFilter();
        TraceIdFilter traceFilter = new TraceIdFilter(() -> "trace-error", WebPlusConstants.TRACE_ID_HEADER);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/system/users"));

        Mono<Void> result = traceFilter.filter(exchange, tracedExchange -> filter.filter(tracedExchange,
                forwardedExchange -> Mono.error(new IllegalStateException("downstream failed"))));

        assertThatThrownBy(result::block).isInstanceOf(IllegalStateException.class);
        assertThat(exchange.getResponse().getHeaders().getFirst(WebPlusConstants.TRACE_ID_HEADER))
                .isEqualTo("trace-error");
    }
}
