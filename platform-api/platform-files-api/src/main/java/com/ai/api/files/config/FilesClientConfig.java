package com.ai.api.files.config;

import com.ai.api.context.TenantExecutionScope;
import com.ai.api.files.client.FilesClient;
import com.ai.api.files.dto.FileBusinessFileDTO;
import com.ai.api.files.dto.FileUploadResultDTO;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import io.github.guanxiangkai.web.plus.security.client.TenantForwardingExchangeFilterFunction;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 平台文件服务内部客户端自动配置。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration",
        "org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerClientAutoConfiguration"
})
@EnableConfigurationProperties({TrustedForwardProperties.class, FilesClientProperties.class})
public class FilesClientConfig {

    /**
     * 创建基于 Nacos 服务发现的文件客户端。
     *
     * @param lbFunction 负载均衡过滤器
     * @param trustedForwardProperties 内部可信转发配置
     * @param webClientBuilder WebClient 构建器
     * @return 文件服务客户端
     */
    @Bean
    @ConditionalOnBean({LoadBalancedExchangeFilterFunction.class, WebClient.Builder.class})
    @ConditionalOnMissingBean(FilesClient.class)
    public FilesClient filesClient(LoadBalancedExchangeFilterFunction lbFunction,
                                   TrustedForwardProperties trustedForwardProperties,
                                   FilesClientProperties filesClientProperties,
                                   WebClient.Builder webClientBuilder) {
        trustedForwardProperties.validateConfigured("platform-files 内部客户端");

        WebClient webClient = webClientBuilder.clone()
                .baseUrl("http://platform-files")
                .defaultHeader(AuthConstants.HeaderConstants.USER_ID,
                        AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID)
                .defaultHeader(trustedForwardProperties.getHeaderName(), trustedForwardProperties.getToken())
                .filter(new TenantForwardingExchangeFilterFunction(
                        TenantExecutionScope::currentTenantId))
                .filter(lbFunction)
                .build();

        return new FilesClient() {
            @Override
            public FileUploadResultDTO upload(Resource resource, String filename, String contentType,
                                              String bizType, String bizId) {
                try {
                    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
                    bodyBuilder.part("file", resource)
                            .filename(filename)
                            .contentType(MediaType.parseMediaType(contentType));

                    Map<?, ?> response = webClient.post()
                            .uri(uriBuilder -> uriBuilder.path("/internal/files/upload")
                                    .queryParam("bizType", bizType)
                                    .queryParam("bizId", bizId)
                                    .build())
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block(filesClientProperties.getUploadTimeout());

                    if (response == null || !Integer.valueOf(200).equals(response.get("code"))) {
                        throw new IllegalStateException("文件上传失败");
                    }
                    Object dataObject = response.get("data");
                    if (!(dataObject instanceof Map<?, ?> data)) {
                        throw new IllegalStateException("文件上传返回数据为空");
                    }
                    return new FileUploadResultDTO(
                            stringValue(data.get("fileId")),
                            stringValue(data.get("originName")),
                            stringValue(data.get("storeName")),
                            stringValue(data.get("contentType")),
                            longValue(data.get("size")),
                            stringValue(data.get("url")),
                            stringValue(data.get("hash"))
                    );
                } catch (Exception exception) {
                    throw new IllegalStateException("调用平台文件上传接口失败", exception);
                }
            }

            @Override
            public List<FileBusinessFileDTO> activeBusinessFiles(String bizType, String bizId) {
                Map<?, ?> response = businessFileRequest(webClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/internal/files/business-files")
                                .queryParam("bizType", bizType)
                                .queryParam("bizId", bizId)
                                .build()));
                Object dataObject = response.get("data");
                if (!(dataObject instanceof List<?> data)) {
                    throw new IllegalStateException("查询业务附件返回数据非法");
                }
                return data.stream().map(item -> {
                    if (!(item instanceof Map<?, ?> file)) {
                        throw new IllegalStateException("查询业务附件返回文件数据非法");
                    }
                    return new FileBusinessFileDTO(
                            stringValue(file.get("fileId")),
                            stringValue(file.get("filename")));
                }).toList();
            }

            @Override
            public void recycleBusinessFiles(String bizType, String bizId) {
                businessFileRequest(webClient.post()
                        .uri(uriBuilder -> uriBuilder.path("/internal/files/business-files/recycle")
                                .queryParam("bizType", bizType)
                                .queryParam("bizId", bizId)
                                .build()));
            }

            private Map<?, ?> businessFileRequest(WebClient.RequestHeadersSpec<?> request) {
                Map<?, ?> response = request.retrieve()
                        .bodyToMono(Map.class)
                        .block(filesClientProperties.getBusinessFileOperationTimeout());
                if (response == null || !Integer.valueOf(200).equals(response.get("code"))) {
                    throw new IllegalStateException("业务附件操作失败");
                }
                return response;
            }

            @Override
            public Mono<ResponseEntity<Flux<DataBuffer>>> download(String fileId) {
                return webClient.get()
                        .uri("/internal/files/{fileId}", fileId)
                        .retrieve()
                        .toEntityFlux(DataBuffer.class)
                        .timeout(filesClientProperties.getDownloadResponseTimeout());
            }
        };
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.parseLong(String.valueOf(value));
    }
}
