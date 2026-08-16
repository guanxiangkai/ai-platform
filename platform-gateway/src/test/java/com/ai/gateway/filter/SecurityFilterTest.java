package com.ai.gateway.filter;

import com.ai.gateway.config.AiGatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityFilterTest {

    @Test
    void personnelIdQueryParameterDoesNotTriggerXssDetection() {
        SecurityFilter filter = new SecurityFilter(new AiGatewayProperties());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
                "/business/staff/attendance/list?pageNum=1&pageSize=999"
                        + "&personnelId=683bf06c-d7e6-54bd-9f6f-897bc2b7f2bb"
                        + "&attendanceType=leave&startDate=2026-05-04&endDate=2026-05-10"
                        + "&timestamp=1778650121873"
        ).build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void eventHandlerAttributeStillTriggersXssDetection() {
        SecurityFilter filter = new SecurityFilter(new AiGatewayProperties());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/business/staff/attendance/list")
                .queryParam("keyword", "onerror=alert(1)")
                .build());

        filter.filter(exchange, chainExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
