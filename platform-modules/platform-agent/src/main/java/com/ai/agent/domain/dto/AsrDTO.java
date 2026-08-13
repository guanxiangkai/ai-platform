package com.ai.agent.domain.dto;

/**
 * ASR 的稳定响应契约。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public final class AsrDTO {
    private AsrDTO() {
    }

    /** 对浏览器稳定返回的转写结果。 */
    public record AsrResult(String recordId, String text, String language) {
    }
}
