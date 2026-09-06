package com.ai.agent.domain.vo;

import java.util.List;

/**
 * 用户端智能体会话的 SSE 数据契约。
 *
 * @param type 事件类型：start、delta、replace、complete 或 error
 * @param id 已持久化的助手消息标识
 * @param invocationId 调用幂等标识
 * @param sessionId 会话标识
 * @param content 增量、替换或完整回答文本
 * @param thoughtProcess 思考过程摘要
 * @param sourceGroups 回答引用来源分组
 * @param message 错误说明
 */
public record AgentSessionStreamEvent(
        String type,
        String id,
        String invocationId,
        String sessionId,
        String content,
        List<String> thoughtProcess,
        List<AgentSessionAskResponse.SourceGroup> sourceGroups,
        String message
) {
    /** 创建开始事件。 */
    public static AgentSessionStreamEvent start(String invocationId, String sessionId) {
        return new AgentSessionStreamEvent(
                "start", null, invocationId, sessionId, null, null, null, null);
    }

    /** 创建增量事件。 */
    public static AgentSessionStreamEvent delta(
            String invocationId, String sessionId, String content) {
        return new AgentSessionStreamEvent(
                "delta", null, invocationId, sessionId, content, null, null, null);
    }

    /** 创建全文替换事件。 */
    public static AgentSessionStreamEvent replace(
            String invocationId, String sessionId, String content) {
        return new AgentSessionStreamEvent(
                "replace", null, invocationId, sessionId, content, null, null, null);
    }

    /** 创建完成事件，字段与普通问答响应保持一致。 */
    public static AgentSessionStreamEvent complete(AgentSessionAskResponse response) {
        return new AgentSessionStreamEvent(
                "complete", response.id(), response.invocationId(), response.sessionId(),
                response.content(), response.thoughtProcess(), response.sourceGroups(), null);
    }

    /** 创建错误事件。 */
    public static AgentSessionStreamEvent error(
            String invocationId, String sessionId, String message) {
        return new AgentSessionStreamEvent(
                "error", null, invocationId, sessionId, null, null, null, message);
    }
}
