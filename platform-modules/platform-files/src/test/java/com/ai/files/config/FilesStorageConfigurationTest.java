package com.ai.files.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilesStorageConfigurationTest {

    private final FilesStorageConfiguration configuration = new FilesStorageConfiguration();

    @ParameterizedTest
    @CsvSource({
            "true,objects.example.invalid,/attachments/reports/a%20b.txt",
            "false,attachments.objects.example.invalid,/reports/a%20b.txt"
    })
    void presignerUsesConfiguredEndpointAddressingAndSigningRegion(
            boolean pathStyle, String expectedHost, String expectedPath) {
        // 仅使用虚构凭据做本地签名，不连接存储服务或读取默认凭据链。
        FilesOssProperties properties = new FilesOssProperties(
                "https://objects.example.invalid:9443", "test-access-key", "test-secret-key",
                "attachments", "eu-west-1", pathStyle);

        try (var presigner = configuration.filesS3Presigner(properties)) {
            var request = presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(5))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(properties.resolvedBucket())
                            .key("reports/a b.txt")
                            .build())
                    .build());

            assertThat(request.url().getProtocol()).isEqualTo("https");
            assertThat(request.url().getHost()).isEqualTo(expectedHost);
            assertThat(request.url().getPort()).isEqualTo(9443);
            assertThat(request.url().getPath()).isEqualTo(expectedPath);
            assertThat(request.httpRequest().rawQueryParameters())
                    .containsEntry("X-Amz-Algorithm", List.of("AWS4-HMAC-SHA256"))
                    .containsEntry("X-Amz-Expires", List.of("300"));
            assertThat(request.httpRequest().rawQueryParameters().get("X-Amz-Credential"))
                    .singleElement().asString().endsWith("/eu-west-1/s3/aws4_request");
            assertThat(request.httpRequest().rawQueryParameters().get("X-Amz-Signature"))
                    .singleElement().asString().matches("[0-9a-f]{64}");
        }
    }

    @Test
    void presignerRejectsMissingCredentialsBeforeCreatingSdkClient() {
        FilesOssProperties properties = new FilesOssProperties(
                "https://objects.example.invalid", null, null, "attachments", "eu-west-1", true);

        assertThatThrownBy(() -> configuration.filesS3Presigner(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OSS 访问凭据未配置");
    }
}
