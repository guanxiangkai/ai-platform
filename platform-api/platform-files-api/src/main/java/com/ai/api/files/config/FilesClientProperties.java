package com.ai.api.files.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 平台文件内部客户端的调用时限。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Validated
@ConfigurationProperties(prefix = "platform.files.client")
public class FilesClientProperties {

    @NotNull
    private Duration uploadTimeout = Duration.ofMinutes(5);

    @NotNull
    private Duration downloadResponseTimeout = Duration.ofMinutes(5);

    @NotNull
    private Duration businessFileOperationTimeout = Duration.ofSeconds(30);

    public Duration getUploadTimeout() {
        return uploadTimeout;
    }

    public void setUploadTimeout(Duration uploadTimeout) {
        this.uploadTimeout = positive(uploadTimeout, "上传超时");
    }

    public Duration getDownloadResponseTimeout() {
        return downloadResponseTimeout;
    }

    public void setDownloadResponseTimeout(Duration downloadResponseTimeout) {
        this.downloadResponseTimeout = positive(downloadResponseTimeout, "下载响应超时");
    }

    /** 获取业务附件查询和回收操作的最大等待时长。 */
    public Duration getBusinessFileOperationTimeout() {
        return businessFileOperationTimeout;
    }

    /** 设置业务附件查询和回收操作的最大等待时长。 */
    public void setBusinessFileOperationTimeout(Duration businessFileOperationTimeout) {
        this.businessFileOperationTimeout = positive(businessFileOperationTimeout, "业务附件操作超时");
    }

    private static Duration positive(Duration value, String label) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(label + "必须大于 0");
        }
        return value;
    }
}
