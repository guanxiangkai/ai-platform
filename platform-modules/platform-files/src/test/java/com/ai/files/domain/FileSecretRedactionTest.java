package com.ai.files.domain;

import com.ai.files.config.FilesOssProperties;
import com.ai.files.domain.dto.FileRequests;
import com.ai.files.domain.vo.FileViews;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FileSecretRedactionTest {

    @Test
    void shouldRedactObjectStorageConfiguration() {
        FilesOssProperties properties = new FilesOssProperties(
                "https://storage.internal.example",
                "access-key-secret",
                "secret-key-secret",
                "bucket-secret",
                "region-1",
                true
        );

        assertThat(properties.toString())
                .contains("connection=<redacted>")
                .doesNotContain(
                        "storage.internal.example", "access-key-secret", "secret-key-secret", "bucket-secret");
    }

    @Test
    void shouldRedactEditLockTokens() {
        FileRequests.ReleaseLock request = new FileRequests.ReleaseLock("request-token-secret");
        FileViews.EditLock response = new FileViews.EditLock(
                "node-id",
                "response-token-secret",
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThat(request.toString())
                .contains("token=<redacted>")
                .doesNotContain("request-token-secret");
        assertThat(response.toString())
                .contains("token=<redacted>")
                .doesNotContain("response-token-secret");
    }
}
