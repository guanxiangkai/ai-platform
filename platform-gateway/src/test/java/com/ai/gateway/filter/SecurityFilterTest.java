package com.ai.gateway.filter;

import com.ai.gateway.config.AiGatewayProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SecurityFilterTest {

    @Test
    void ordinaryQueryParametersDoNotTriggerXssDetection() {
        SecurityFilter filter = new SecurityFilter(new AiGatewayProperties());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
                "/catalog/items?pageNum=1&pageSize=100"
                        + "&subjectId=subject-1"
                        + "&status=active&startDate=2026-05-04&endDate=2026-05-10"
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
                .get("/catalog/items")
                .queryParam("keyword", "onerror=alert(1)")
                .build());

        filter.filter(exchange, chainExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void maliciousQueryIsBlockedWithoutWritingQueryValuesToSecurityLog(CapturedOutput output) {
        SecurityFilter filter = new SecurityFilter(new AiGatewayProperties());
        String token = "token-7f9aaf2d-3148-4ca8-b97b-75a86fd85c22";
        String phone = "13800138000";
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/catalog/items")
                .queryParam("accessToken", token)
                .queryParam("phone", phone)
                .queryParam("keyword", "onerror=alert(1)")
                .build());
        filter.filter(exchange, chainExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(output.getAll())
                .contains("/catalog/items", "参数 [keyword]", "XSS")
                .doesNotContain(token, phone, "onerror=alert(1)");
    }

    @Test
    void overlongParameterNameIsBoundedInSecurityLog(CapturedOutput output) {
        SecurityFilter filter = new SecurityFilter(new AiGatewayProperties());
        String parameterName = "keyword" + "x".repeat(9_000);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/catalog/items")
                .queryParam(parameterName, "onerror=alert(1)")
                .build());
        filter.filter(exchange, chainExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(output.getAll())
                .contains("…", "QUERY_PARAMETER_TOO_LONG")
                .doesNotContain(parameterName);
    }

    @Test
    void unicodeLineSeparatorsCannotForgeMultilineSecurityLog(CapturedOutput output) {
        SecurityFilter filter = new SecurityFilter(new AiGatewayProperties());
        String parameterName = "keyword\u2028spoof\u2029entry";
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/catalog/items")
                .queryParam(parameterName, "onerror=alert(1)")
                .build());

        filter.filter(exchange, chainExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(output.getAll())
                .contains("参数 [keyword?spoof?entry]", "XSS")
                .doesNotContain("\u2028", "\u2029");
    }
}
