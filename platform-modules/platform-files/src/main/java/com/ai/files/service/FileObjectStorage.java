package com.ai.files.service;

import io.github.guanxiangkai.web.plus.error.exception.BizException;
import com.ai.api.files.dto.FileAccessUrlDTO;
import com.ai.files.config.FilesOssProperties;
import com.ai.files.config.FilesProperties;
import com.ai.files.domain.entity.FileVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 对象存储访问边界，文件权限和元数据由上层服务处理。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class FileObjectStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FilesOssProperties ossProperties;
    private final FilesProperties filesProperties;
    private final AtomicBoolean bucketReady = new AtomicBoolean();

    /**
     * 写入一个不可变文件版本。
     *
     * @param path 本地临时文件
     * @param objectKey 对象键
     * @param contentType 媒体类型
     */
    public void put(Path path, String objectKey, String contentType) {
        ensureBucket();
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(ossProperties.resolvedBucket())
                        .key(objectKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromFile(path));
    }

    /**
     * 流式读取文件版本。
     *
     * @param version 文件版本
     * @return 包含下载响应头的文件流
     */
    public ResponseEntity<Resource> download(FileVersion version) {
        ensureBucket();
        try {
            ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(ossProperties.resolvedBucket())
                    .key(version.getObjectKey())
                    .build());
            InputStreamResource resource = new InputStreamResource(stream) {
                @Override
                public String getFilename() {
                    return version.getOriginalName();
                }
            };
            return ResponseEntity.ok()
                    .contentType(parseMediaType(version.getContentType()))
                    .contentLength(version.getSizeBytes())
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(version.getOriginalName(), StandardCharsets.UTF_8)
                            .build().toString())
                    .body(resource);
        } catch (S3Exception exception) {
            throw mapS3Exception(exception);
        }
    }

    /**
     * 生成短时下载地址。
     *
     * @param version 文件版本
     * @return 带失效时间的预签名地址
     */
    public FileAccessUrlDTO accessUrl(FileVersion version) {
        ensureBucket();
        var ttl = filesProperties.resolvedAccessUrlTtl();
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(ossProperties.resolvedBucket())
                .key(version.getObjectKey())
                .responseContentDisposition(ContentDisposition.attachment()
                        .filename(version.getOriginalName(), StandardCharsets.UTF_8)
                        .build().toString())
                .build();
        Instant expiresAt = Instant.now().plus(ttl);
        String url = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .getObjectRequest(request)
                        .build())
                .url().toString();
        return new FileAccessUrlDTO(url, expiresAt);
    }

    /** 判断不可变对象是否已存在。 */
    public boolean exists(String objectKey) {
        ensureBucket();
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(ossProperties.resolvedBucket())
                    .key(objectKey)
                    .build());
            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }

    /**
     * 幂等删除未发布的对象。
     *
     * <p>删除失败必须抛出，调用方依据持久化 saga 继续重试，不得静默丢失。</p>
     */
    public void delete(String objectKey) {
        ensureBucket();
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(ossProperties.resolvedBucket())
                .key(objectKey)
                .build());
    }

    private synchronized void ensureBucket() {
        if (bucketReady.get()) {
            return;
        }
        String bucket = ossProperties.resolvedBucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw exception;
            }
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
        bucketReady.set(true);
    }

    private MediaType parseMediaType(String contentType) {
        try {
            return StringUtils.hasText(contentType)
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (IllegalArgumentException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private RuntimeException mapS3Exception(S3Exception exception) {
        String errorCode = exception.awsErrorDetails() == null
                ? "" : exception.awsErrorDetails().errorCode();
        if (exception.statusCode() == 404 || "NoSuchKey".equals(errorCode)) {
            return new ResponseStatusException(NOT_FOUND, "文件内容不存在");
        }
        return new BizException("读取对象存储失败");
    }
}
