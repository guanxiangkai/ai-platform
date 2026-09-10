package com.ai.api.files.dto;

/** 单文件上传目录及其授权截止时间；不包含对象存储凭据。 */
public record FileBrowserUploadTargetDTO(String spaceId, String parentId, java.time.Instant expiresAt, FileUploadResultDTO uploadedFile) implements java.io.Serializable {
    @java.io.Serial private static final long serialVersionUID = 1L;
}
