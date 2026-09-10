package com.ai.api.files.dto;

/** 可信业务服务为一个确定原件申请指定用户的上传目标。 */
public record FileBrowserUploadTargetRequestDTO(String businessType, String businessId, String uploaderUserId, String filename, long sizeBytes, String sha256) implements java.io.Serializable {
    @java.io.Serial private static final long serialVersionUID = 1L;
}
