package com.ai.api.files.config;

import com.ai.api.files.client.FilesClient;
import com.ai.api.files.dto.FileBusinessFileDTO;
import com.ai.api.files.dto.FileUploadResultDTO;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * platform-files HTTP 适配器。
 *
 * <p>文件服务的统一响应由 Jackson 按 {@link ApiResponse} 的泛型参数直接解码，
 * 不再维护 Map 到 DTO 的手工映射。</p>
 */
final class HttpFilesClient implements FilesClient {

    private static final ParameterizedTypeReference<ApiResponse<FileUploadResultDTO>> UPLOAD_RESPONSE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<List<FileBusinessFileDTO>>> BUSINESS_FILES_RESPONSE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<Void>> EMPTY_RESPONSE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final FilesClientProperties properties;

    HttpFilesClient(WebClient webClient, FilesClientProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @Override
    public FileUploadResultDTO upload(Resource resource, String filename, String contentType,
                                      String bizType, String bizId) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", resource)
                .filename(filename)
                .contentType(MediaType.parseMediaType(contentType));

        ApiResponse<FileUploadResultDTO> response = request(
                webClient.post()
                        .uri(uriBuilder -> uriBuilder.path("/internal/files/upload")
                                .queryParam("bizType", bizType)
                                .queryParam("bizId", bizId)
                                .build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(body.build())),
                UPLOAD_RESPONSE, properties.getUploadTimeout(), "上传");
        return requiredData(response, "文件上传");
    }

    @Override
    public List<FileBusinessFileDTO> activeBusinessFiles(String bizType, String bizId) {
        ApiResponse<List<FileBusinessFileDTO>> response = request(
                businessFilesRequest(webClient.get(), "/internal/files/business-files", bizType, bizId),
                BUSINESS_FILES_RESPONSE, properties.getBusinessFileOperationTimeout(), "查询业务附件");
        List<FileBusinessFileDTO> files = requiredData(response, "查询业务附件");
        if (files.stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("查询业务附件返回文件数据非法");
        }
        return List.copyOf(files);
    }

    @Override
    public void recycleBusinessFiles(String bizType, String bizId) {
        request(businessFilesRequest(webClient.post(), "/internal/files/business-files/recycle", bizType, bizId),
                EMPTY_RESPONSE, properties.getBusinessFileOperationTimeout(), "回收业务附件");
    }

    @Override
    public Mono<ResponseEntity<Flux<DataBuffer>>> download(String fileId) {
        return webClient.get()
                .uri("/internal/files/{fileId}", fileId)
                .retrieve()
                .toEntityFlux(DataBuffer.class)
                .timeout(properties.getDownloadResponseTimeout());
    }

    private static WebClient.RequestHeadersSpec<?> businessFilesRequest(WebClient.RequestHeadersUriSpec<?> request,
                                                                          String path, String bizType, String bizId) {
        return request.uri(uriBuilder -> uriBuilder.path(path)
                .queryParam("bizType", bizType)
                .queryParam("bizId", bizId)
                .build());
    }

    /** 统一校验 HTTP、业务状态和空响应；回收操作允许业务数据为空。 */
    private static <T> ApiResponse<T> request(WebClient.RequestHeadersSpec<?> request,
                                              ParameterizedTypeReference<ApiResponse<T>> responseType,
                                              Duration timeout, String operation) {
        ApiResponse<T> response;
        try {
            response = request.retrieve().bodyToMono(responseType).block(timeout);
        } catch (Exception exception) {
            throw new IllegalStateException("调用平台文件" + operation + "接口失败", exception);
        }
        if (response == null) {
            throw new IllegalStateException(operation + "响应为空");
        }
        if (!response.isSuccess()) {
            throw new IllegalStateException(operation + "失败：" + response.message());
        }
        return response;
    }

    private static <T> T requiredData(ApiResponse<T> response, String operation) {
        if (response.data() == null) {
            throw new IllegalStateException(operation + "返回数据为空");
        }
        return response.data();
    }
}
