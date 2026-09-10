package com.ai.files.config;

/** 浏览器并发上传的受控策略；业务方仍须按自身原件容量取更严格的限制。 */
@org.springframework.boot.context.properties.ConfigurationProperties(prefix="platform.files.browser-upload")
public record BrowserUploadProperties(Integer concurrency) {
    public BrowserUploadProperties {
        concurrency = concurrency == null ? 3 : concurrency;
        if (concurrency < 1 || concurrency > 8) throw new IllegalArgumentException("浏览器上传并发数必须在1到8之间");
    }
}
