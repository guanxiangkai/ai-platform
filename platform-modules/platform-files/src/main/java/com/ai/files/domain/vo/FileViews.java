package com.ai.files.domain.vo;

import java.time.Instant;

/** 平台文件专用响应契约。 */
public final class FileViews {

    private FileViews() {
    }

    /** 编辑锁明文令牌只在获取或续期成功时返回一次。 */
    public record EditLock(String nodeId, String token, Instant expiresAt) {
        /** 返回不包含编辑锁令牌的诊断摘要。 */
        @Override
        public String toString() {
            return "EditLock[nodeId=" + nodeId + ", token=<redacted>, expiresAt=" + expiresAt + ']';
        }
    }

    /** 空间配额使用情况。 */
    public record SpaceUsage(String spaceId, long quotaBytes, long usedBytes, long availableBytes) {
    }
}
