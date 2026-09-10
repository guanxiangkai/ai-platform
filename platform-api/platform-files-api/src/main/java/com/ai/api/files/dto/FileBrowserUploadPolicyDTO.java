package com.ai.api.files.dto;

/** 浏览器业务文件上传的服务端资源策略。 */
public record FileBrowserUploadPolicyDTO(int concurrency, long maxFileSizeBytes) implements java.io.Serializable {
    @java.io.Serial private static final long serialVersionUID = 1L;
}
