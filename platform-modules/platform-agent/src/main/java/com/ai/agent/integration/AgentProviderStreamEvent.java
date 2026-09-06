package com.ai.agent.integration;

/**
 * 提供方流式调用的统一事件。
 *
 * @param type 事件类型
 * @param content 增量或完整替换文本
 * @param result 上游完成后的统一结果
 */
public record AgentProviderStreamEvent(
        Type type,
        String content,
        AgentProviderResult result
) {
    /** 提供方流式事件类型。 */
    public enum Type {
        /** 新增回答片段。 */
        DELTA,
        /** 覆盖当前回答全文。 */
        REPLACE,
        /** 上游调用完成。 */
        COMPLETE
    }

    /** 创建增量事件。 */
    public static AgentProviderStreamEvent delta(String content) {
        return new AgentProviderStreamEvent(Type.DELTA, content, null);
    }

    /** 创建全文替换事件。 */
    public static AgentProviderStreamEvent replace(String content) {
        return new AgentProviderStreamEvent(Type.REPLACE, content, null);
    }

    /** 创建完成事件。 */
    public static AgentProviderStreamEvent complete(AgentProviderResult result) {
        return new AgentProviderStreamEvent(Type.COMPLETE, null, result);
    }
}
