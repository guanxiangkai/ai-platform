package com.ai.agent.domain.vo;

import java.util.List;

/**
 * 用户端通用智能体会话响应。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public record AgentSessionAskResponse(
        String id,
        String invocationId,
        String sessionId,
        String content,
        List<String> thoughtProcess,
        List<SourceGroup> sourceGroups
) {
    /**
     * 一组具有相同业务语义的回答来源。
     *
     * @author guanxiangkai
     * @since 1.0.0
     */
    public record SourceGroup(String id, String title, int count, List<Source> sources) {
    }

    /**
     * 回答引用的单个受控来源。
     *
     * @author guanxiangkai
     * @since 1.0.0
     */
    public record Source(String id, String title, String label, String path, String meta) {
    }
}
