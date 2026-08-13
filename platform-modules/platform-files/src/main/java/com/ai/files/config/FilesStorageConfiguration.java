package com.ai.files.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 平台文件服务的 S3 客户端配置。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties({FilesOssProperties.class, FilesProperties.class})
public class FilesStorageConfiguration {

    /** 创建同步 S3 客户端。 */
    @Bean(destroyMethod = "close")
    public S3Client filesS3Client(FilesOssProperties properties, FilesProperties filesProperties) {
        properties.validate();
        return S3Client.builder()
                .endpointOverride(properties.resolvedEndpoint())
                .credentialsProvider(credentials(properties))
                .region(Region.of(properties.resolvedRegion()))
                .serviceConfiguration(serviceConfiguration(properties))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(filesProperties.resolvedObjectOperationTimeout())
                        .build())
                .build();
    }

    /** 创建 S3 预签名客户端。 */
    @Bean(destroyMethod = "close")
    public S3Presigner filesS3Presigner(FilesOssProperties properties) {
        properties.validate();
        return S3Presigner.builder()
                .endpointOverride(properties.resolvedEndpoint())
                .credentialsProvider(credentials(properties))
                .region(Region.of(properties.resolvedRegion()))
                .serviceConfiguration(serviceConfiguration(properties))
                .build();
    }

    private StaticCredentialsProvider credentials(FilesOssProperties properties) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                properties.accessKey().trim(), properties.secretKey().trim()));
    }

    private S3Configuration serviceConfiguration(FilesOssProperties properties) {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(properties.resolvedPathStyleAccessEnabled())
                .build();
    }
}
