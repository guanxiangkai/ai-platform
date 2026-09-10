package com.ai.api.files.dto;

/** 可信业务服务确认原件版本及预约身份，平台原子封存后供业务处理。 */
public record FileBrowserUploadAcceptRequestDTO(String businessType, String businessId, String uploaderUserId, String uploadParentId, String fileId, String versionId, String filename, long sizeBytes, String sha256) implements java.io.Serializable {
    @java.io.Serial private static final long serialVersionUID = 1L;
}
