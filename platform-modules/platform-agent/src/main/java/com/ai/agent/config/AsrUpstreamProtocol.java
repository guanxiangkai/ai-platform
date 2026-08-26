package com.ai.agent.config;

/**
 * ASR 上游请求协议。
 */
public enum AsrUpstreamProtocol {
    /** OpenAI 兼容的 multipart/form-data 请求。 */
    OPENAI_MULTIPART,
    /** 阿里云百炼 Qwen Audio 多模态同步 JSON 请求。 */
    DASHSCOPE_MULTIMODAL
}
