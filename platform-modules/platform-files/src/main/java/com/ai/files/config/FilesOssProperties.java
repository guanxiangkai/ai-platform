package com.ai.files.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.net.URI;

/** SeaweedFS S3 兼容网关配置。 */
@ConfigurationProperties(prefix = "oss")
public record FilesOssProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String region,
        Boolean pathStyleAccessEnabled
) {

    /**
     * 校验并返回 S3 网关地址。
     *
     * @return S3 网关 URI
     */
    public URI resolvedEndpoint() {
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("OSS 网关地址未配置");
        }
        return URI.create(endpoint.trim());
    }

    /** 返回对象桶名称。 */
    public String resolvedBucket() {
        return StringUtils.hasText(bucket) ? bucket.trim() : "ai-platform";
    }

    /** 返回 S3 区域。 */
    public String resolvedRegion() {
        return StringUtils.hasText(region) ? region.trim() : "us-east-1";
    }

    /** 返回是否启用路径风格访问。 */
    public boolean resolvedPathStyleAccessEnabled() {
        return pathStyleAccessEnabled == null || pathStyleAccessEnabled;
    }

    /** 校验对象存储访问凭据。 */
    public void validate() {
        resolvedEndpoint();
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("OSS 访问凭据未配置");
        }
    }

    /**
     * 返回不包含网关地址、存储桶和访问凭据的诊断摘要。
     *
     * @return 已脱敏的对象存储配置摘要
     */
    @Override
    public String toString() {
        return "FilesOssProperties[connection=<redacted>, region=" + resolvedRegion()
                + ", pathStyleAccessEnabled=" + resolvedPathStyleAccessEnabled() + ']';
    }
}
