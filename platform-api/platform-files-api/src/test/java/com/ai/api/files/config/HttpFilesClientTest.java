package com.ai.api.files.config;

import com.ai.api.context.TenantExecutionScope;
import com.ai.api.files.client.FilesClient;
import com.ai.api.files.dto.FileBusinessFileDTO;
import com.ai.api.files.dto.FileUploadResultDTO;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpFilesClientTest {

    @Test
    void uploadShouldDecodeTypedDtoAndPreserveMultipartRequestHeaders() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        FilesClient client = client(request -> {
            captured.set(request);
            return json(HttpStatus.OK, """
                    {"code":200,"message":"ok","data":{"fileId":"file-1","originName":"a.txt",
                    "storeName":"stored","contentType":"text/plain","size":9223372036854775806,
                    "url":"/files/file-1","hash":"hash"},"timestamp":1}
                    """);
        });

        FileUploadResultDTO result = client.upload(new ByteArrayResource("content".getBytes()),
                "a.txt", "text/plain", "invoice", "order-1");

        assertThat(result.fileId()).isEqualTo("file-1");
        assertThat(result.size()).isEqualTo(9_223_372_036_854_775_806L);
        assertThat(captured.get().method().name()).isEqualTo("POST");
        assertThat(captured.get().url().getPath()).isEqualTo("/internal/files/upload");
        assertThat(captured.get().url().getQuery()).contains("bizType=invoice", "bizId=order-1");
        assertThat(captured.get().headers().getContentType().isCompatibleWith(MediaType.MULTIPART_FORM_DATA)).isTrue();
        assertThat(multipartBody(captured.get()))
                .contains("filename=\"a.txt\"", "Content-Type: text/plain", "content");
    }

    @Test
    void activeBusinessFilesShouldDecodeTypedListAndAllowEmptyList() {
        FilesClient client = client(request -> json(HttpStatus.OK, """
                {"code":200,"message":"ok","data":[{"fileId":"file-1","filename":"a.txt"}],"timestamp":1}
                """));
        List<FileBusinessFileDTO> files = client.activeBusinessFiles("invoice", "order-1");
        assertThat(files)
                .containsExactly(new FileBusinessFileDTO("file-1", "a.txt"));
        assertThatThrownBy(() -> files.add(new FileBusinessFileDTO("file-2", "b.txt")))
                .isInstanceOf(UnsupportedOperationException.class);

        FilesClient emptyClient = client(request -> json(HttpStatus.OK,
                "{\"code\":200,\"message\":\"ok\",\"data\":[],\"timestamp\":1}"));
        assertThat(emptyClient.activeBusinessFiles("invoice", "order-1")).isEmpty();
    }

    @Test
    void activeBusinessFilesShouldRejectNullElementsAndMissingData() {
        FilesClient nullElementClient = client(request -> json(HttpStatus.OK,
                "{\"code\":200,\"message\":\"ok\",\"data\":[null],\"timestamp\":1}"));
        assertThatThrownBy(() -> nullElementClient.activeBusinessFiles("invoice", "order-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("文件数据非法");

        FilesClient missingDataClient = client(request -> json(HttpStatus.OK,
                "{\"code\":200,\"message\":\"ok\",\"data\":null,\"timestamp\":1}"));
        assertThatThrownBy(() -> missingDataClient.activeBusinessFiles("invoice", "order-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("返回数据为空");

        FilesClient objectDataClient = client(request -> json(HttpStatus.OK,
                "{\"code\":200,\"message\":\"ok\",\"data\":{\"fileId\":\"file-1\"},\"timestamp\":1}"));
        assertThatThrownBy(() -> objectDataClient.activeBusinessFiles("invoice", "order-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("调用平台文件查询业务附件接口失败")
                .hasCauseInstanceOf(Exception.class);

        FilesClient uploadWithoutDataClient = client(request -> json(HttpStatus.OK,
                "{\"code\":200,\"message\":\"ok\",\"data\":null,\"timestamp\":1}"));
        assertThatThrownBy(() -> uploadWithoutDataClient.upload(new ByteArrayResource(new byte[0]),
                "a.txt", "text/plain", "invoice", "order-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("文件上传返回数据为空");
    }

    @Test
    void typedOperationsShouldRejectBusinessHttpAndEmptyResponsesButAllowRecycleWithoutData() {
        FilesClient businessFailureClient = client(request -> json(HttpStatus.OK,
                "{\"code\":500,\"message\":\"denied\",\"data\":null,\"timestamp\":1}"));
        assertThatThrownBy(() -> businessFailureClient.recycleBusinessFiles("invoice", "order-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("denied");

        FilesClient httpFailureClient = client(request -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
        assertThatThrownBy(() -> httpFailureClient.activeBusinessFiles("invoice", "order-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("调用平台文件查询业务附件接口失败")
                .hasCauseInstanceOf(Exception.class);

        FilesClient emptyResponseClient = client(request -> Mono.just(ClientResponse.create(HttpStatus.OK).build()));
        assertThatThrownBy(() -> emptyResponseClient.activeBusinessFiles("invoice", "order-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("响应为空");

        FilesClient recycleClient = client(request -> json(HttpStatus.OK,
                "{\"code\":200,\"message\":\"ok\",\"data\":null,\"timestamp\":1}"));
        recycleClient.recycleBusinessFiles("invoice", "order-1");
    }

    @Test
    void operationTimeoutShouldCancelTheRequestAndPreserveFailureCause() {
        AtomicInteger cancellations = new AtomicInteger();
        FilesClientProperties shortTimeout = properties();
        shortTimeout.setBusinessFileOperationTimeout(Duration.ofMillis(10));
        FilesClient client = new HttpFilesClient(WebClient.builder()
                .exchangeFunction(request -> Mono.<ClientResponse>never().doOnCancel(cancellations::incrementAndGet))
                .build(), shortTimeout);

        assertThatThrownBy(() -> client.activeBusinessFiles("invoice", "order-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("调用平台文件查询业务附件接口失败")
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(cancellations.get()).isEqualTo(1);
    }

    @Test
    void configurationShouldKeepTrustedAndTenantHeaders() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction exchange = request -> {
            captured.set(request);
            return json(HttpStatus.OK, "{\"code\":200,\"message\":\"ok\",\"data\":[],\"timestamp\":1}");
        };
        LoadBalancedExchangeFilterFunction loadBalancer = passthroughLoadBalancer();
        TrustedForwardProperties trusted = new TrustedForwardProperties();
        trusted.setToken("trusted-token");
        FilesClient client = new FilesClientConfig().filesClient(loadBalancer, trusted, properties(),
                WebClient.builder().exchangeFunction(exchange));

        TenantExecutionScope.run("tenant-1", () -> client.activeBusinessFiles("invoice", "order-1"));

        assertThat(captured.get().headers().getFirst(AuthConstants.HeaderConstants.USER_ID))
                .isEqualTo(AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID);
        assertThat(captured.get().headers().getFirst(trusted.getHeaderName())).isEqualTo("trusted-token");
        assertThat(captured.get().headers().getFirst(AuthConstants.HeaderConstants.TENANT_ID)).isEqualTo("tenant-1");
    }

    @Test
    void downloadShouldDeferBodyConsumptionAndPropagateCancellation() {
        AtomicInteger exchanges = new AtomicInteger();
        AtomicInteger bodySubscriptions = new AtomicInteger();
        AtomicInteger cancellations = new AtomicInteger();
        DataBuffer first = DefaultDataBufferFactory.sharedInstance.wrap(new byte[]{1});
        DataBuffer second = DefaultDataBufferFactory.sharedInstance.wrap(new byte[]{2});
        FilesClient client = client(request -> {
            exchanges.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .body(Flux.just(first, second)
                            .doOnSubscribe(ignored -> bodySubscriptions.incrementAndGet())
                            .doOnCancel(cancellations::incrementAndGet))
                    .build());
        });

        Mono<org.springframework.http.ResponseEntity<Flux<DataBuffer>>> download = client.download("file-1");
        assertThat(exchanges.get()).isZero();
        assertThat(bodySubscriptions.get()).isZero();

        org.springframework.http.ResponseEntity<Flux<DataBuffer>> response = download.block();
        assertThat(exchanges.get()).isEqualTo(1);
        assertThat(bodySubscriptions.get()).isZero();
        assertThat(cancellations.get()).isZero();
        response.getBody().take(1).blockLast();
        assertThat(bodySubscriptions.get()).isEqualTo(1);
        assertThat(cancellations.get()).isEqualTo(1);
    }

    private static FilesClient client(ExchangeFunction exchangeFunction) {
        return new HttpFilesClient(WebClient.builder().exchangeFunction(exchangeFunction).build(), properties());
    }

    private static String multipartBody(ClientRequest request) {
        MockClientHttpRequest output = new MockClientHttpRequest(request.method(), request.url());
        output.getHeaders().putAll(request.headers());
        request.body().insert(output, new BodyInserter.Context() {
            @Override
            public List<HttpMessageWriter<?>> messageWriters() {
                return ExchangeStrategies.withDefaults().messageWriters();
            }

            @Override
            public Optional<ServerHttpRequest> serverRequest() {
                return Optional.empty();
            }

            @Override
            public Map<String, Object> hints() {
                return Map.of();
            }
        }).block();
        return output.getBodyAsString().block();
    }

    private static FilesClientProperties properties() {
        FilesClientProperties properties = new FilesClientProperties();
        properties.setUploadTimeout(Duration.ofSeconds(1));
        properties.setBusinessFileOperationTimeout(Duration.ofSeconds(1));
        properties.setDownloadResponseTimeout(Duration.ofSeconds(1));
        return properties;
    }

    private static Mono<ClientResponse> json(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    private static LoadBalancedExchangeFilterFunction passthroughLoadBalancer() {
        LoadBalancedExchangeFilterFunction loadBalancer = mock(LoadBalancedExchangeFilterFunction.class);
        when(loadBalancer.filter(any(ClientRequest.class), any(ExchangeFunction.class)))
                .thenAnswer(invocation -> invocation.<ExchangeFunction>getArgument(1)
                        .exchange(invocation.getArgument(0)));
        return loadBalancer;
    }
}
